import java.util.function.Function;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSNumber;

public class PricingService {

    @JS(args = {"handler"}, value = "globalThis.pricingService = handler;")
    private static native void export(Function<JSObject, JSObject> handler);

    public static void main(String[] args) {
        export((request) -> {


            String operation = (String) request.get("operation");
            int price = ((JSNumber) request.get("price")).asInt();

            JSObject user = (JSObject) request.get("user");
            String name = (String) user.get("name");
            boolean premium = (boolean) user.get("premium");

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
        });
    }
}