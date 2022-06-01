package converter.view;

import converter.business.converters.JSON;
import converter.business.converters.UniversalConverter;
import converter.business.converters.XML;
import converter.business.tokenizer.JsonTokenizer;
import converter.business.tokenizer.XmlTokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Facade {

    public static void run() {
        String input;
        try {
            input = Files.readString(Paths.get("test.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String output = switch (defineFormat(input)) {
            case "JSON" -> toXML(input);
            case "XML" -> toJSON(input);
            default -> throw new IllegalStateException("Unsupported format");
        };
        System.out.println(output);
    }

    private static String toJSON(String input) {
        Object xmlTokens = new XmlTokenizer(input).getMap();
        JSON jsonConverter = new JSON(xmlTokens);
        return jsonConverter.parse();
    }

    private static String toXML(String input) {
        Object jsonTokens = new JsonTokenizer(input).getMap();
        XML xmlConverter = new XML(jsonTokens);
        String formatted = xmlConverter.parse();
        return new UniversalConverter(formatted).toXml();
    }

    private static String defineFormat(String input) {
        if (input.charAt(0) == '{')
            return "JSON";
        else if (input.charAt(0) == '<')
            return "XML";
        else
            throw new IllegalStateException("Unsupported format");
    }
}
