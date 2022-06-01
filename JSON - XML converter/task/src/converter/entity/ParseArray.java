package converter.entity;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

public class ParseArray implements Iterable<Object> {

    private final ArrayList<Object> arrayList;

    public ParseArray() {
        this.arrayList = new ArrayList<>();
    }

    public ParseArray(ParseArray pa) {
        this.arrayList = pa.arrayList;
    }

    @Override
    public Iterator<Object> iterator() {
        return this.arrayList.iterator();
    }


    public ParseArray put(Object value) {
        this.arrayList.add(value);
        return this;
    }

    @Override
    public String toString() {
        return arrayList.toString();
    }

    public boolean isEmpty() {
        return this.arrayList.size() == 0;
    }

    public ParseArray filterInput() {
        ParseArray result = new ParseArray();
        Object value;

        for (Object entry : arrayList) {
            value = entry;
            if (value instanceof ParseObject) {
                value = new ParseObject((ParseObject) value).filterInput("element");
                if (((ParseObject) value).isEmpty())
                    value = "";
            } else if (value instanceof ParseArray) {
                value = new ParseArray((ParseArray) value).filterInput();
                if (((ParseArray) value).isEmpty())
                    value = "";
            }
            result.put(value);
        }
        return result;
    }

    public void write(Writer writer, int indentFactor, int indent) {
        try {
            boolean needsComma = false;
            int length = this.arrayList.size();
            writer.write('[');

            if (length == 1) {
                try {
                    ParseObject.writeValue(writer, this.arrayList.get(0),
                            indentFactor, indent);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to write JSONArray value at index: 0", e);
                }
            } else if (length != 0) {
                final int newIndent = indent + indentFactor;

                for (int i = 0; i < length; i += 1) {
                    if (needsComma) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    ParseObject.indent(writer, newIndent);
                    try {
                        ParseObject.writeValue(writer, this.arrayList.get(i),
                                indentFactor, newIndent);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to write JSONArray value at index: " + i, e);
                    }
                    needsComma = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                ParseObject.indent(writer, indent);
            }
            writer.write(']');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object filterJsonInput() {
        ParseArray result = new ParseArray();
        Object value;
        for (Object o : arrayList) {
            value = o;
            if (value instanceof ParseObject) {
                value = new ParseObject((ParseObject) value).filterJsonInput("element");
            } else if (value instanceof ParseArray) {
                value = new ParseArray((ParseArray) value).filterJsonInput();
                return value;
            }
            result.put(value);
        }

        return result;
    }
}
