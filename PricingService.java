import java.util.function.Function;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSBoolean;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSNumber;
import org.graalvm.webimage.api.JSString;

public class PricingService {

    @JS(args = {"handler"}, value = "globalThis.pricingService = handler;")
    private static native void export(Function<JSObject, JSObject> handler);

    public static void main(String[] args) {
        export((request) -> {
            try {
                String operation = ((JSString) request.get("operation")).asString();
                int price = ((JSNumber) request.get("price")).asInt();

                JSObject user = (JSObject) request.get("user");
                String name = ((JSString) user.get("name")).asString();
                boolean premium = ((JSBoolean) user.get("premium")).asBoolean();

                int discount = 0;

                if ("discount".equals(operation)) {
                    discount = premium ? 20 : 10;
                }

                int finalPrice = price - (price * discount / 100);

                JSObject response = JSObject.create();
                response.set("finalPrice", JSNumber.of(finalPrice));
                response.set("discountApplied", JSNumber.of(price - finalPrice));
                response.set("user", name);
                response.set("premium", premium);
                return response;
            } catch (Throwable t) {
                System.err.println("PricingService exception:");
                t.printStackTrace();

                JSObject error = JSObject.create();
                error.set("error", "PricingService failed");
                return error;
            }
        });
    }
}
