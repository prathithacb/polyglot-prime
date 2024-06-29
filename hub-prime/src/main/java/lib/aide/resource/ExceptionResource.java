package lib.aide.resource;

import java.util.Optional;
import java.util.function.Supplier;

import lib.aide.paths.PathSuffixes;

public class ExceptionResource implements TextResource<ExceptionNature> {
    static public final EmptyResource SINGLETON = new EmptyResource();

    private final Supplier<String> src;
    private final ExceptionNature nature;
    private final Optional<PathSuffixes> suffixes;

    public ExceptionResource(final Exception exception) {
        this.src = () -> exception.toString();
        this.nature = new ExceptionNature();
        this.suffixes = Optional.empty();
    }

    public ExceptionResource(final String src, ExceptionNature nature, Optional<PathSuffixes> suffixes) {
        this.src = () -> src;
        this.nature = nature;
        this.suffixes = suffixes;
    }

    public ExceptionResource(final Supplier<String> src, ExceptionNature nature, Optional<PathSuffixes> suffixes) {
        this.src = src;
        this.nature = nature;
        this.suffixes = suffixes;
    }

    @Override
    public ExceptionNature nature() {
        return nature;
    }

    @Override
    public String content() {
        return src.get();
    }

    public Optional<PathSuffixes> suffixes() {
        return suffixes;
    }
}

class ExceptionNature implements Nature {
    @Override
    public String mimeType() {
        return "text/plain";
    }
}
