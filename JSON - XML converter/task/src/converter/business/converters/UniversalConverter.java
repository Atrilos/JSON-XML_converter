package converter.business.converters;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UniversalConverter {

    private final ArrayDeque<String> pathDeque = new ArrayDeque<>();
    private final Matcher universalMatcher;
    private int depth = 0;
    private StringBuilder sb = new StringBuilder();

    public UniversalConverter(String input) {
        universalMatcher = Pattern
                .compile("Element:\\vpath = (.*)\\v(value = (.*)\\v?)?(attributes:\\v((.* = .*\\v)+))?")
                .matcher(input);
    }

    public String toJson() {
        sb.append("{\n");
        int bracketCounter = 0;

        while (universalMatcher.find()) {
            List<String> newPath = pathToList(universalMatcher.group(1));
            String value = universalMatcher.group(3);
            Map<String, String> attributes = universalMatcher.group(5) == null ?
                    new LinkedHashMap<>() : mapAttributes(universalMatcher.group(5));

            if (newPath.size() < depth) {
                int length = newPath.size();
                for (; depth - length > 0; depth--, bracketCounter--) {
                    if (depth - length == 1)
                        sb.append("}");
                    else
                        sb.append("}\n");
                }
                sb.append(",\n");
            } else if (newPath.size() == depth) {
                sb.append(",\n");
            }
            depth = newPath.size();

            if (!attributes.isEmpty()) {
                sb.append('"').append(newPath.get(newPath.size() - 1)).append("\": {\n");
                bracketCounter++;
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    sb.append('"')
                            .append('@').append(entry.getKey()).append('"')
                            .append(": ")
                            .append(entry.getValue()).append(",\n");
                }
                sb.append('"').append('#').append(newPath.get(newPath.size() - 1)).append("\": ");
            } else {
                sb.append('"').append(newPath.get(newPath.size() - 1)).append("\": ");
            }

            if (Objects.equals(value, "null")) {
                sb.append("null");
            } else if (Objects.equals(value, "")) {
                sb.append("\"\"");
            } else if (value != null) {
                sb.append(value);
            } else {
                sb.append("{\n");
                bracketCounter++;
            }
            if (!attributes.isEmpty() && value != null) {
                sb.append("\n}");
                bracketCounter--;
            }
        }

        while (bracketCounter > 0) {
            sb.append("}\n");
            bracketCounter--;
        }

        sb.append("}");
        return sb.toString();
    }

    public String toXml() {
        boolean oneTopElement = true;
        String firstTopElement = null;

        while (universalMatcher.find()) {
            List<String> newPath = pathToList(universalMatcher.group(1));
            String value = universalMatcher.group(3);
            Map<String, String> attributes = universalMatcher.group(5) == null ?
                    new LinkedHashMap<>() : mapAttributes(universalMatcher.group(5));

            //oneTopElement check
            if (firstTopElement == null) {
                firstTopElement = newPath.get(0);
            } else {
                if (!firstTopElement.equals(newPath.get(0)))
                    oneTopElement = false;
            }

            if (newPath.size() <= pathDeque.size()) {
                if (newPath.size() < pathDeque.size()) {
                    int depth = pathDeque.size() - newPath.size();
                    for (; depth >= 0; depth--) {
                        sb.append(pathDeque.pollLast());
                    }
                } else {
                    if (!Objects.equals(newPath.get(newPath.size() - 1), pathDeque.peekLast()))
                        sb.append(pathDeque.pollLast());
                }
            }
            sb.append("<").append(newPath.get(newPath.size() - 1));
            pathDeque.offer("</" + newPath.get(newPath.size() - 1) + ">\n");

            if (!attributes.isEmpty()) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    sb.append(" ")
                            .append(entry.getKey()).append("=")
                            .append(entry.getValue());
                }
            }

            if (Objects.equals(value, "null")) {
                sb.append(" />").append("\n");
                pathDeque.pollLast();
            } else if (Objects.equals(value, "\"\""))
                sb.append(">").append(pathDeque.pollLast());
            else if (value != null) {
                value = value.substring(1, value.length() - 1);
                sb.append(">").append(value);
            } else if (sb.toString().charAt(sb.length() - 1) != '\n')
                sb.append(">").append("\n");
        }

        while (!pathDeque.isEmpty()) {
            sb.append(pathDeque.pollLast());
        }

        if (!oneTopElement) {
            String tmp = sb.toString();
            sb = new StringBuilder();
            sb.append("<root>\n").append(tmp).append("</root>");
        }
        return sb.toString();
    }

    private List<String> pathToList(String path) {
        Matcher m = Pattern.compile("([^,\\s]+)\\b").matcher(path);
        List<String> paths = new ArrayList<>();
        while (m.find()) {
            if (!Objects.equals(m.group(), ""))
                paths.add(m.group());
        }
        return paths;
    }

    private Map<String, String> mapAttributes(String attr) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(.*?) = (.*)\\v?").matcher(attr);
        while (m.find())
            map.put(m.group(1), m.group(2));
        return map;
    }
}
