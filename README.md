# Web Image: Export Java Method with JavaScript Objects

Unlike the [Export Java Method Example](../export-java-function/) which exchanges primitive values like numbers, this demo illustrates how you can work with **structured data objects**: JavaScript sends a request object, Java processes it, and returns a structured response object, via WebAssembly.
What is important with the current Web Image API, for exchanging objects you need to **cast values explicitly** and then convert wrapper values to Java primitives/strings.

This tiny in-browser service is compiled to WebAssembly thanks to  **Web Image** - an experimental backend for [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) that compiles a Java application ahead-of-time and produces a Wasm module with a JavaScript wrapper.
You can run this small backend service in a browser, Node.js, or on the [GraalJS-based](https://github.com/oracle/graaljs/tree/master/graal-nodejs) Node runtime.

> Note: Web Image is an experimental technology and under active development. APIs, tooling, and capabilities may change.

## Prerequisites

* An [Early Access build](https://github.com/graalvm/oracle-graalvm-ea-builds/releases) of Oracle GraalVM 25 (25e1) or later.
* All [prerequisites](https://www.graalvm.org/latest/reference-manual/native-image/#prerequisites) required for Native Image building.
* [Binaryen toolchain](https://github.com/WebAssembly/binaryen) version 119 or later, available on the system path. Web Image uses `wasm-as` from `binaryen` as its assembler.
  * **macOS**: It is recommended to install Binaryen using Homebrew, as the pre-built binaries from GitHub may be quarantined by the operating system:
    ```bash
    brew install binaryen
    ```
  * **Other platforms**: Download a pre-built release for your platform from [GitHub](https://github.com/WebAssembly/binaryen/releases).

## Building the WebAssembly Module

1. Compile the Java source file:
    ```bash
    javac PricingService.java
    ```

2. Compile the application to Wasm by passing the `--tool:svm-wasm` option (it should be the first argument):
    ```bash
    native-image --tool:svm-wasm -H:-AutoRunVM PricingService
    ```
    The `-H:-AutoRunVM` option prevents the VM from starting `main()` automatically. Calling `GraalVM.run` from JavaScript ensures that exported function is available after the runtime initializes.

    The build produces the following artifacts:

    * _pricingservice.js_ — JavaScript runtime wrapper
    * _pricingservice.js.wasm_ — the compiled WebAssembly module
    * _pricingservice.js.wat_ — debug artifact showing the generated WebAssembly text format

3. Run the application in a browser using a simple HTTP server:
    ```bash
    python3 -m http.server 8000
    ```
    ```bash
    jwebserver -p 8000
    ```
    Navigate to [http://localhost:8000](http://localhost:8000), and click the button in the demo page to send a request object to Java and display the processed result.

## Review the Sample Application

This example exports a Java function that behaves like a small pricing service.
JavaScript sends a request object, Java processes it, and returns a response object.

```java
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
        });
    }
}
```

### Annotating the Function

* The `@JS` annotation exposes a Java function to JavaScript. It is part of [GraalVM Web Image API](https://www.graalvm.org/sdk/javadoc/org/graalvm/webimage/api/JS.html).
* `args = {"handler"}` defines the variable name available in JavaScript.
* The JavaScript snippet assigns the function to `globalThis.pricingService`, and makes it callable from JavaScript once the runtime is ready.

### Exporting a Method

```java
export((request) -> { ... });
```
* The exported function is a Java lambda and accepts a JavaScript object (`Function<JSObject, JSObject>`).
* JavaScript sends an object, Java processes it, creates, and returns another object.

### Reading Properties from a Request Object

Next Java reads properties from a JavaScript `request` object.
For accessing JavaScript objects, you need to **cast values explicitly**, then convert wrappers to Java values.

```java
String operation = ((JSString) request.get("operation")).asString();
int price = ((JSNumber) request.get("price")).asInt();

JSObject user = (JSObject) request.get("user");
String name = ((JSString) user.get("name")).asString();
boolean premium = ((JSBoolean) user.get("premium")).asBoolean();
```
Notice also that nested objects can also be accessed.
The `request` object is passed to Java as a **JavaScript proxy object**.

### Creating a Response Object

After reading the properties, it creates a `response` object in Java.
This object becomes a normal JavaScript object when returned.
```java
JSObject response = JSObject.create();
response.set("finalPrice", JSNumber.of(finalPrice));
```

### Calling the Java Function from JavaScript

In the HTML file you see:
```js
<script>
GraalVM.run([]).then(() => {
    const button = document.getElementById("run");
    const output = document.getElementById("output");

    button.addEventListener("click", () => {
        const priceInput = parseInt(document.getElementById("price").value, 10);
        const premiumInput = document.getElementById("premium").checked;

        const request = new Object();
        request.operation = "discount";
        request.price = Number.isNaN(priceInput) ? 0 : priceInput;

        request.user = new Object();
        request.user.name = "Alex";
        request.user.premium = premiumInput;

        const result = globalThis.pricingService(request);
        output.innerText = JSON.stringify(result, null, 2);
    });
});
</script>
```

* `GraalVM.run([])` initializes the WebAssembly runtime.
* After initialization, `globalThis.pricingService(...)` becomes available.
* The Java code runs inside WebAssembly and returns the result.

## Current Web Image API: Important Notes and Limitations

- For accessing JavaScript objects, you need to **cast values explicitly**.
    - Cast `request.get("field")` to wrapper types: `JSString`, `JSBoolean`, `JSNumber`, or `JSObject`.
    - Then convert wrappers with `.asString()`, `.asBoolean()`, `.asInt()`, or `.asDouble()`.
    - For `JSObject` nested fields, call `get()` and cast again.

- The generated runtime wrapper should be loaded in HTML before the `GraalVM.run` call:
    ```html
    <script src="pricingservice.js"></script>
    ```

- Current Web Image API works with object literals and runtime-created JavaScript objects.
Thus using `new Object()` ensures the object is represented in a way the Java expects:
    ```js
    const priceInput = parseInt(document.getElementById("price").value, 10);
    const premiumInput = document.getElementById("premium").checked;

    const request = new Object();
    request.operation = "discount";
    request.price = Number.isNaN(priceInput) ? 0 : priceInput;

    request.user = new Object();
    request.user.name = "Alex";
    request.user.premium = premiumInput;
    ```

### Conclusion

Thanks to Web Image, a mini Java backend service can be embedded into a webpage, and made callable like a JavaScript function.
Web Image enables:
- Passing structured objects from JavaScript to Java
- Accessing nested object properties in Java
- Returning complex objects back to JavaScript
- Running Java logic as a WebAssembly module inside the browser

To conclude, Web Image can support realistic data exchange patterns.
