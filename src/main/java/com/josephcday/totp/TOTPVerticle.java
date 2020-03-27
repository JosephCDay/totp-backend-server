package com.josephcday.totp;

import java.io.IOException;

import com.google.zxing.WriterException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

/**
 * TOTP Server Verticle
 * 
 * @author Joseph Curtis Day
 */
public class TOTPVerticle extends AbstractVerticle {
    private HttpServer server;
    private static final Logger logger = LoggerFactory.getLogger(TOTPVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        OpenAPI3RouterFactory.create(this.vertx, "src/main/resources/totp.yaml", ar -> {
            if (ar.succeeded()) {
                OpenAPI3RouterFactory routerFactory = ar.result();

                // curl http://localhost:8080/secret
                // {"secret":"RF4T5GRSSFNIL6LX"} // Random
                routerFactory.addHandlerByOperationId("secretGET", routingContext -> {
                    String b32Secret = TOTP.b32Secret();
                    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json").setStatusCode(200)
                            .end(new JsonObject().put("secret", b32Secret).encode());
                });

                // curl http://localhost:8080/token/QB5UDBW7OQKYYDZU

                // curl http://localhost:8080/token/QB5UDBW7OQKYYDZU?unixtime=158524245
                // {"token":"937384"}
                routerFactory.addHandlerByOperationId("tokenGET", routingContext -> {
                    RequestParameters params = routingContext.get("parsedParameters");
                    RequestParameter secret = params.pathParameter("secret");
                    RequestParameter millisec = params.queryParameter("millisec");
                    RequestParameter unixtime = params.queryParameter("unixtime");
                    try {
                        String totp;
                        if (millisec != null) {
                            totp = TOTP.getToken(secret.toString(), millisec.getLong());
                        } else if (unixtime != null) {
                            totp = TOTP.getToken(secret.toString(), unixtime.getInteger());
                        } else {
                            totp = TOTP.getToken(secret.toString());
                        }
                        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                .setStatusCode(200).end(new JsonObject().put("token", totp).encode());
                    } catch (Exception e) {
                        logger.warn("400: " + routingContext.request().uri());
                        JsonObject errorObject = new JsonObject();
                        errorObject.put("code", 400).put("message", "Bad Request");
                        routingContext.response().setStatusCode(400)
                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(errorObject.encode());
                    }
                });

                // curl
                // http://localhost:8080/check?secret=QB5UDBW7OQKYYDZU&token=937384&unixtime=158524245
                // 200
                // curl
                // http://localhost:8080/check?secret=QB5UDBW7OQKYYDZU&token=825785
                // 401
                routerFactory.addHandlerByOperationId("checkGET", routingContext -> {
                    RequestParameters params = routingContext.get("parsedParameters");
                    RequestParameter secret = params.queryParameter("secret"); // contract prevents null
                    RequestParameter token = params.queryParameter("token"); // contract prevents null
                    RequestParameter millisec = params.queryParameter("millisec");
                    RequestParameter unixtime = params.queryParameter("unixtime");
                    RequestParameter window = params.queryParameter("window");
                    try {
                        boolean checkToken;
                        if (millisec != null) {
                            checkToken = TOTP.validate(secret.toString(), token.getInteger(), millisec.getLong(),
                                    window != null ? window.getInteger() : 0);
                        } else if (unixtime != null) {
                            checkToken = TOTP.validate(secret.toString(), token.getInteger(), unixtime.getInteger(),
                                    window != null ? window.getInteger() : 0);
                        } else {
                            checkToken = TOTP.validate(secret.toString(), token.getInteger(),
                                    window != null ? window.getInteger() : 0);
                        }
                        if (checkToken) {
                            routingContext.response().setStatusCode(200).end();
                        } else {
                            routingContext.response().setStatusCode(401).end();
                            logger.warn("failed: " + routingContext.request().uri());
                        }
                    } catch (Exception e) {
                        logger.warn("400: " + routingContext.request().uri());
                        JsonObject errorObject = new JsonObject();
                        errorObject.put("code", 400).put("message", "Bad Request");
                        routingContext.response().setStatusCode(400)
                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(errorObject.encode());
                    }
                });

                // curl \
                // http://localhost:8080/image?secret=QB5UDBW7OQKYYDZU&size=256&label=QRTest
                // binary image
                routerFactory.addHandlerByOperationId("imageGET", routingContext -> {
                    RequestParameters params = routingContext.get("parsedParameters");
                    RequestParameter label = params.queryParameter("label"); // contract prevents null
                    RequestParameter secret = params.queryParameter("secret"); // contract prevents null
                    RequestParameter size = params.queryParameter("size");
                    String url = TOTP.generateUrl(label.toString(), secret.toString());
                    Buffer image;

                    try {
                        if (size != null) {
                            image = QR.generateQRCodeImage(url, size.getInteger());
                        } else {
                            image = QR.generateQRCodeImage(url);
                        }
                        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/png")
                                .putHeader(HttpHeaders.CACHE_CONTROL, "private,no-cache,no-store")
                                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(image.length()))
                                .setStatusCode(200).write(image).end();
                    } catch (WriterException | IOException e) {
                        logger.warn("400: " + routingContext.request().uri());
                        JsonObject errorObject = new JsonObject();
                        errorObject.put("code", 400).put("message", "Bad Request");
                        routingContext.response().setStatusCode(400)
                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(errorObject.encode());
                    }

                });

                Router router = routerFactory.getRouter();
                router.errorHandler(404, routingContext -> {
                    logger.warn("404: " + routingContext.request().uri());
                    JsonObject errorObject = new JsonObject();
                    errorObject.put("code", 404).put("message",
                            (routingContext.failure() != null) ? routingContext.failure().getMessage() : "Not Found");
                    routingContext.response().setStatusCode(404).putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .end(errorObject.encode());
                });
                router.errorHandler(400, routingContext -> {
                    logger.warn("400: " + routingContext.request().uri());
                    JsonObject errorObject = new JsonObject();
                    errorObject.put("code", 400).put("message",
                            (routingContext.failure() != null) ? routingContext.failure().getMessage() : "Bad Request");
                    routingContext.response().setStatusCode(400).putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .end(errorObject.encode());
                });
                server = vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true)
                        .setPort(EnvVars._appPort).setIdleTimeout(EnvVars._timeout));
                server.requestHandler(router).listen();
                logger.info("TOTPVerticle listening on port " + server.actualPort());
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });

    }

    /**
     * Halts the TOTPVerticle server
     */
    @Override
    public void stop() {
        this.server.close();
    }

    /**
     * Starts an embeded TOTPVerticle server
     * 
     * @param params none used
     */
    public static void main(String[] params) {
        EnvVars.init();
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new TOTPVerticle());
    }
}
