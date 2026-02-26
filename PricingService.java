import java.util.function.Function;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSString;

public class PricingService {

    @JS(args = {"handler"}, value = "globalThis.pricingService = handler;")
    private static native void export(Function<JSString, JSString> handler);

    public static void main(String[] args) {
        export((jsonRequest) -> {
            // Receive JSON string from JS
            String requestStr = jsonRequest.toString();

            // For demonstration, we'll just parse key-values manually
            // This simple parser works for our demo object:
            boolean premium = requestStr.contains("\"premium\":true");
            int price = Integer.parseInt(requestStr.replaceAll(".*\"price\":(\\d+).*", "$1"));

            int discount = premium ? 20 : 10;
            int finalPrice = price - (price * discount / 100);

            // Return JSON string to JS
            String response = "{"
                    + "\"finalPrice\":" + finalPrice + ","
                    + "\"discountApplied\":" + (price - finalPrice) + ","
                    + "\"premium\":" + premium
                    + "}";
            return JSString.of(response);
        });
    }
}