package io.hyperfoil.core.handlers;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.InitFromParam;
import io.hyperfoil.api.config.Locator;
import io.hyperfoil.api.processor.Processor;
import io.hyperfoil.api.processor.Transformer;
import io.hyperfoil.api.session.ResourceUtilizer;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.builders.ServiceLoadedBuilderProvider;
import io.hyperfoil.core.data.DataFormat;
import io.hyperfoil.core.generators.Pattern;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class JsonParser implements Serializable, ResourceUtilizer {
   protected static final Logger log = LoggerFactory.getLogger(JsonParser.class);
   protected static final int MAX_PARTS = 16;

   protected final String query;
   protected final boolean delete;
   protected final Transformer replace;
   protected final Processor processor;
   private final JsonParser.Selector[] selectors;

   public JsonParser(String query, boolean delete, Transformer replace, Processor processor) {
      this.query = query;
      this.delete = delete;
      this.replace = replace;
      this.processor = processor;

      byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);
      if (queryBytes.length == 0 || queryBytes[0] != '.') {
         throw new BenchmarkDefinitionException("Path should start with '.'");
      }
      ArrayList<Selector> selectors = new ArrayList<>();
      int next = 1;
      for (int i = 1; i < queryBytes.length; ++i) {
         if (queryBytes[i] == '[' || queryBytes[i] == '.' && next < i) {
            while (queryBytes[next] == '.') ++next;
            if (next != i) {
               selectors.add(new AttribSelector(Arrays.copyOfRange(queryBytes, next, i)));
            }
            next = i + 1;
         }
         if (queryBytes[i] == '[') {
            ArraySelector arraySelector = new ArraySelector();
            ++i;
            int startIndex = i, endIndex = i;
            for (; i < queryBytes.length; ++i) {
               if (queryBytes[i] == ']') {
                  if (endIndex < i) {
                     arraySelector.rangeEnd = bytesToInt(queryBytes, startIndex, i);
                     if (startIndex == endIndex) {
                        arraySelector.rangeStart = arraySelector.rangeEnd;
                     }
                  }
                  selectors.add(arraySelector);
                  next = i + 1;
                  break;
               } else if (queryBytes[i] == ':') {
                  if (startIndex < i) {
                     arraySelector.rangeStart = bytesToInt(queryBytes, startIndex, i);
                  }
                  endIndex = i + 1;
               }
            }
         }
      }
      if (next < queryBytes.length) {
         while (queryBytes[next] == '.') ++next;
         selectors.add(new AttribSelector(Arrays.copyOfRange(queryBytes, next, queryBytes.length)));
      }
      this.selectors = selectors.toArray(new JsonParser.Selector[0]);
   }

   private boolean isModifying() {
      return delete || replace != null;
   }

   protected abstract void fireMatch(Context context, Session session, ByteStream data, int offset, int length, boolean isLastPart);

   private static int bytesToInt(byte[] bytes, int start, int end) {
      int value = 0;
      for (; ; ) {
         if (bytes[start] < '0' || bytes[start] > '9') {
            throw new BenchmarkDefinitionException("Invalid range specification: " + new String(bytes));
         }
         value += bytes[start] - '0';
         if (++start >= end) {
            return value;
         } else {
            value *= 10;
         }
      }
   }

   @Override
   public void reserve(Session session) {
      ResourceUtilizer.reserve(session, processor);
   }

   interface Selector extends Serializable {
      Context newContext();

      interface Context {
         void reset();
      }
   }

   private static class AttribSelector implements JsonParser.Selector {
      byte[] name;

      AttribSelector(byte[] name) {
         this.name = name;
      }

      boolean match(ByteStream data, int start, int end, int offset) {
         assert start <= end;
         for (int i = 0; i < name.length && i < end - start; ++i) {
            if (name[i + offset] != data.getByte(start + i)) return false;
         }
         return true;
      }

      @Override
      public Context newContext() {
         return null;
      }
   }

   private static class ArraySelector implements Selector {
      int rangeStart = 0;
      int rangeEnd = Integer.MAX_VALUE;

      @Override
      public Context newContext() {
         return new ArraySelectorContext();
      }

      boolean matches(ArraySelectorContext context) {
         return context.active && context.currentItem >= rangeStart && context.currentItem <= rangeEnd;
      }
   }

   private static class ArraySelectorContext implements Selector.Context {
      boolean active;
      int currentItem;

      @Override
      public void reset() {
         active = false;
         currentItem = 0;
      }
   }

   protected class Context implements Session.Resource {
      Selector.Context[] selectorContext = new Selector.Context[selectors.length];
      int level;
      int selectorLevel;
      int selector;
      boolean inQuote;
      boolean inAttrib;
      boolean escaped;
      int keyStartPart;
      int keyStartIndex;
      int lastCharPart;
      int lastCharIndex;
      int valueStartPart;
      int valueStartIndex;
      int lastOutputPart;
      int lastOutputIndex;
      ByteStream[] parts = new ByteStream[MAX_PARTS];
      int nextPart;
      ByteStream[] pool = new ByteStream[MAX_PARTS];

      protected Context(Function<Context, ByteStream> byteStreamSupplier) {
         for (int i = 0; i < pool.length; ++i) {
            pool[i] = byteStreamSupplier.apply(this);
         }
         for (int i = 0; i < selectors.length; ++i) {
            selectorContext[i] = selectors[i].newContext();
         }
         reset();
      }

      public void reset() {
         for (Selector.Context ctx : selectorContext) {
            if (ctx != null) ctx.reset();
         }
         level = -1;
         selectorLevel = 0;
         selector = 0;
         inQuote = false;
         inAttrib = false;
         escaped = false;
         keyStartPart = -1;
         keyStartIndex = -1;
         lastCharPart = -1;
         lastCharIndex = -1;
         valueStartPart = -1;
         valueStartIndex = -1;
         lastOutputPart = -1;
         lastOutputIndex = 0;
         nextPart = 0;
         for (int i = 0; i < parts.length; ++i) {
            if (parts[i] == null) break;
            parts[i].release();
            parts[i] = null;
         }
      }

      private Selector.Context current() {
         return selectorContext[selector];
      }

      public void parse(ByteStream data, Session session) {
         while (data.isReadable()) {
            byte b = data.readByte();
            switch (b) {
               case ' ':
               case '\n':
               case '\t':
               case '\r':
                  // ignore whitespace
                  break;
               case '\\':
                  escaped = !escaped;
                  break;
               case '{':
                  if (!inQuote) {
                     ++level;
                     inAttrib = true;
                     // TODO assert we have active attrib selector
                  }
                  break;
               case '}':
                  if (!inQuote) {
                     tryRecord(session, data);
                     if (level == selectorLevel) {
                        --selectorLevel;
                        --selector;
                     }
                     --level;
                  }
                  break;
               case '"':
                  if (!escaped) {
                     inQuote = !inQuote;
                  }
                  break;
               case ':':
                  if (!inQuote) {
                     if (selectorLevel == level && keyStartIndex >= 0 && selector < selectors.length && selectors[selector] instanceof AttribSelector) {
                        AttribSelector selector = (AttribSelector) selectors[this.selector];
                        int offset = 0;
                        boolean previousPartsMatch = true;
                        if (keyStartPart >= 0) {
                           int endIndex;
                           if (lastCharPart != keyStartPart) {
                              endIndex = parts[keyStartPart].writerIndex();
                           } else {
                              endIndex = lastCharIndex;
                           }
                           while (previousPartsMatch = selector.match(parts[keyStartPart], keyStartIndex, endIndex, offset)) {
                              offset += endIndex - keyStartIndex;
                              keyStartPart++;
                              keyStartIndex = 0;
                              if (keyStartPart >= parts.length || parts[keyStartPart] == null) {
                                 break;
                              }
                           }
                        }
                        if (previousPartsMatch && (lastCharPart >= 0 || selector.match(data, keyStartIndex, lastCharIndex, offset))) {
                           onMatch(data);
                        }
                     }
                     keyStartIndex = -1;
                     inAttrib = false;
                  }
                  break;
               case ',':
                  if (!inQuote) {
                     inAttrib = true;
                     keyStartIndex = -1;
                     tryRecord(session, data);
                     if (selectorLevel == level && selector < selectors.length && current() instanceof ArraySelectorContext) {
                        ArraySelectorContext asc = (ArraySelectorContext) current();
                        if (asc.active) {
                           asc.currentItem++;
                        }
                        if (((ArraySelector) selectors[selector]).matches(asc)) {
                           onMatch(data);
                        }
                     }
                  }
                  break;
               case '[':
                  if (!inQuote) {
                     ++level;
                     if (selectorLevel == level && selector < selectors.length && selectors[selector] instanceof ArraySelector) {
                        ArraySelectorContext asc = (ArraySelectorContext) current();
                        asc.active = true;
                        if (((ArraySelector) selectors[selector]).matches(asc)) {
                           onMatch(data);
                        }
                     }
                  }
                  break;
               case ']':
                  if (!inQuote) {
                     tryRecord(session, data);
                     if (selectorLevel == level && selector < selectors.length && current() instanceof ArraySelectorContext) {
                        ArraySelectorContext asc = (ArraySelectorContext) current();
                        asc.active = false;
                        --selectorLevel;
                     }
                     --level;
                  }
                  break;
               default:
                  lastCharPart = -1;
                  lastCharIndex = data.readerIndex();
                  if (inAttrib && keyStartIndex < 0) {
                     keyStartPart = -1;
                     keyStartIndex = data.readerIndex() - 1;
                  }
            }
            if (b != '\\') {
               escaped = false;
            }
         }
         if (keyStartIndex >= 0 || valueStartIndex >= 0) {
            if (nextPart == parts.length) {
               log.warn("Too many buffered fragments, dropping data.");
               parts[0].release();
               System.arraycopy(parts, 1, parts, 0, parts.length - 1);
               --nextPart;
               if (keyStartPart == 0 && keyStartIndex >= 0) {
                  keyStartIndex = 0;
               } else if (keyStartPart > 0) {
                  --keyStartPart;
               }
               if (lastCharPart == 0 && lastCharIndex >= 0) {
                  lastCharIndex = 0;
               } else if (lastCharPart > 0) {
                  --lastCharPart;
               }
               if (valueStartPart == 0 && valueStartIndex >= 0) {
                  valueStartIndex = 0;
               } else if (valueStartPart > 0) {
                  --valueStartPart;
               }
            }
            parts[nextPart] = data.retain();
            if (keyStartPart < 0) {
               keyStartPart = nextPart;
            }
            if (lastCharPart < 0) {
               lastCharPart = nextPart;
            }
            if (valueStartPart < 0) {
               valueStartPart = nextPart;
            }
            ++nextPart;
         } else {
            for (int i = 0; i < parts.length && parts[i] != null; ++i) {
               parts[i].release();
               parts[i] = null;
            }
            nextPart = 0;
         }
      }

      private void onMatch(ByteStream data) {
         ++selector;
         if (selector < selectors.length) {
            ++selectorLevel;
         } else {
            valueStartPart = -1;
            valueStartIndex = data.readerIndex();
         }
      }

      private void tryRecord(Session session, ByteStream data) {
         if (selectorLevel == level && valueStartIndex >= 0) {
            // valueStartIndex is always before quotes here
            ByteStream buf = valueStartPart < 0 ? data : parts[valueStartPart];
            buf = tryAdvanceValueStart(data, buf);
            LOOP:
            while (valueStartIndex < buf.writerIndex() || valueStartPart != -1) {
               switch (buf.getByte(valueStartIndex)) {
                  case ' ':
                  case '\n':
                  case '\r':
                  case '\t':
                     ++valueStartIndex;
                     buf = tryAdvanceValueStart(data, buf);
                     break;
                  default:
                     break LOOP;
               }
            }
            int end = data.readerIndex() - 1;
            int endPart = nextPart;
            buf = data;
            if (end == 0) {
               endPart--;
               buf = parts[endPart];
               end = buf.writerIndex();
            }
            LOOP:
            while (end > valueStartIndex || valueStartPart >= 0 && endPart > valueStartPart) {
               switch (buf.getByte(end - 1)) {
                  case ' ':
                  case '\n':
                  case '\r':
                  case '\t':
                     --end;
                     if (end == 0) {
                        if (valueStartPart >= 0 && endPart > valueStartPart) {
                           endPart--;
                           buf = parts[endPart];
                           end = buf.writerIndex();
                        }
                     }
                     break;
                  default:
                     break LOOP;
               }
            }
            if (valueStartIndex == end && (valueStartPart < 0 || valueStartPart == endPart)) {
               // This happens when we try to select from a 0-length array
               // - as long as there are not quotes there's nothing to record.
               valueStartIndex = -1;
               --selector;
               return;
            }
            while (valueStartPart >= 0 && valueStartPart != endPart) {
               int valueEndIndex = parts[valueStartPart].writerIndex();
               fireMatch(this, session, parts[valueStartPart], valueStartIndex, valueEndIndex - valueStartIndex, false);
               incrementValueStartPart();
            }
            fireMatch(this, session, buf(data, endPart), valueStartIndex, end - valueStartIndex, true);
            valueStartIndex = -1;
            --selector;
         }
      }

      private ByteStream tryAdvanceValueStart(ByteStream data, ByteStream buf) {
         if (valueStartIndex >= buf.writerIndex() && valueStartPart >= 0) {
            valueStartPart++;
            if (valueStartPart >= parts.length || (buf = parts[valueStartPart]) == null) {
               buf = data;
               valueStartPart = -1;
            }
            valueStartIndex = 0;
         }
         return buf;
      }

      private void incrementValueStartPart() {
         valueStartIndex = 0;
         valueStartPart++;
         if (valueStartPart >= parts.length || parts[valueStartPart] == null) {
            valueStartPart = -1;
         }
      }

      private ByteStream buf(ByteStream data, int part) {
         if (part < 0 || part >= parts.length || parts[part] == null) {
            return data;
         }
         return parts[part];
      }

      public ByteStream retain(ByteStream stream) {
         for (int i = 0; i < pool.length; ++i) {
            ByteStream pooled = pool[i];
            if (pooled != null) {
               pool[i] = null;
               stream.moveTo(pooled);
               return pooled;
            }
         }
         throw new IllegalStateException();
      }

      public void release(ByteStream stream) {
         for (int i = 0; i < pool.length; ++i) {
            if (pool[i] == null) {
               pool[i] = stream;
               return;
            }
         }
         throw new IllegalStateException();
      }
   }

   public abstract static class BaseBuilder<S extends BaseBuilder<S>> implements InitFromParam<S> {
      protected Locator locator;
      protected String query;
      protected boolean unquote = true;
      protected Processor.Builder<?> processor;
      protected DataFormat format = DataFormat.STRING;
      protected boolean delete;
      protected Transformer.Builder replace;

      /**
       * @param param Either <code>query -&gt; variable</code> or <code>variable &lt;- query</code>.
       * @return Self.
       */
      @Override
      public S init(String param) {
         String query;
         String var;
         if (param.contains("->")) {
            String[] parts = param.split("->");
            query = parts[0];
            var = parts[1];
         } else if (param.contains("<-")) {
            String[] parts = param.split("->");
            query = parts[1];
            var = parts[0];
         } else {
            throw new BenchmarkDefinitionException("Cannot parse json query specification: '" + param + "', use 'query -> var' or 'var <- query'");
         }
         return query(query.trim()).toVar(var.trim());
      }

      @SuppressWarnings("unchecked")
      protected S self() {
         return (S) this;
      }

      public S setLocator(Locator locator) {
         this.locator = locator;
         return self();
      }

      @SuppressWarnings("unchecked")
      public S copy(Locator locator) {
         S copy;
         try {
            copy = (S) getClass().newInstance();
         } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
         }
         return copy.setLocator(locator).query(query).unquote(unquote).processor(processor);
      }

      /**
       * Query selecting the part of JSON.
       *
       * @param query Query.
       * @return Self.
       */
      public S query(String query) {
         this.query = query;
         return self();
      }

      /**
       * Automatically unquote and unescape the input values. By default true.
       *
       * @param unquote Do unquote and unescape?
       * @return Builder.
       */
      public S unquote(boolean unquote) {
         this.unquote = unquote;
         return self();
      }

      /**
       * If this is set to true, the selected key will be deleted from the JSON and the modified JSON will be passed
       * to the <code>processor</code>.
       *
       * @param delete Should the selected query be deleted?
       * @return Self.
       */
      public S delete(boolean delete) {
         this.delete = delete;
         return self();
      }

      /**
       * Custom transformation executed on the value of the selected item.
       * Note that the output value must contain quotes (if applicable) and be correctly escaped.
       *
       * @return Builder.
       */
      public ServiceLoadedBuilderProvider<Transformer.Builder> replace() {
         return new ServiceLoadedBuilderProvider<>(Transformer.Builder.class, locator, this::replace);
      }

      public S replace(Transformer.Builder replace) {
         if (replace == null) {
            throw new BenchmarkDefinitionException("Calling replace twice!");
         }
         this.replace = replace;
         return self();
      }

      /**
       * Replace value of selected item with value generated through a pattern.
       * Note that the result must contain quotes and be correctly escaped.
       *
       * @param pattern Pattern format.
       * @return Self.
       */
      public S replace(String pattern) {
         return replace(fragmented -> new Pattern(pattern, false)).unquote(false);
      }

      /**
       * Shortcut to store selected parts in an array in the session. Must follow the pattern <code>variable[maxSize]</code>
       *
       * @param varAndSize Array name.
       * @return Self.
       */
      public S toArray(String varAndSize) {
         return processor(new ArrayRecorder.Builder().init(varAndSize).format(format));
      }

      /**
       * Shortcut to store first match in given variable. Further matches are ignored.
       *
       * @param var Variable name.
       * @return Self.
       */
      public S toVar(String var) {
         return processor(new SimpleRecorder.Builder().toVar(var).format(format));
      }

      public S processor(Processor.Builder<?> processor) {
         if (this.processor != null) {
            throw new BenchmarkDefinitionException("Processor already set!");
         }
         this.processor = processor;
         return self();
      }

      /**
       * Conversion to apply on the matching parts with 'toVar' or 'toArray' shortcuts.
       *
       * @param format Data format.
       * @return Self.
       */
      public S format(DataFormat format) {
         this.format = format;
         return self();
      }

      protected void validate() {
         if (query == null) {
            throw new BenchmarkDefinitionException("Missing 'query'");
         } else if (processor == null) {
            throw new BenchmarkDefinitionException("Missing processor - use 'processor', 'toVar' or 'toArray'");
         }
      }
   }
}
