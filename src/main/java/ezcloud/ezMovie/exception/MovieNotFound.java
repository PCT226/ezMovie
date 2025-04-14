package ezcloud.ezMovie.exception;

public class MovieNotFound extends RuntimeException {
    public MovieNotFound(String msg) {
        super(msg);
    }
}
