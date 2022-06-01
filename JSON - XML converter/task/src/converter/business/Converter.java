package converter.business;

public abstract class Converter {

    protected Object input;

    public Converter(Object input) {
        this.input = input;
    }

    public abstract String parse();
}
