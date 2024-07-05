package org.example;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

    private MongoClient mongoClient;
    private String dbName = "ecommerce";
    private String productCollection = "products";

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject mongoConfig = new JsonObject()
                .put("connection_string", "mongodb://localhost:27017")
                .put("db_name", dbName);

        mongoClient = MongoClient.createShared(vertx, mongoConfig);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Define routes
        router.post("/api/products").handler(this::addProduct);
        router.get("/api/products").handler(this::getAllProducts);
        router.get("/api/products/:id").handler(this::getProductById);
        router.put("/api/products/:id").handler(this::updateProduct);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void addProduct(RoutingContext context) {
        JsonObject product = context.getBodyAsJson();
        mongoClient.save(productCollection, product, res -> {
            if (res.succeeded()) {
                context.response()
                        .setStatusCode(201)
                        .end(product.put("_id", res.result()).encode());
            } else {
                context.response()
                        .setStatusCode(500)
                        .end(res.cause().getMessage());
            }
        });
    }

    private void getAllProducts(RoutingContext context) {
        mongoClient.find(productCollection, new JsonObject(), res -> {
            if (res.succeeded()) {
                context.response()
                        .setStatusCode(200)
                        .end(new JsonArray(res.result()).encode());
            } else {
                context.response()
                        .setStatusCode(500)
                        .end(res.cause().getMessage());
            }
        });
    }

    private void getProductById(RoutingContext context) {
        String id = context.pathParam("id");
        mongoClient.findOne(productCollection, new JsonObject().put("_id", id), null, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    context.response()
                            .setStatusCode(200)
                            .end(res.result().encode());
                } else {
                    context.response()
                            .setStatusCode(404)
                            .end();
                }
            } else {
                context.response()
                        .setStatusCode(500)
                        .end(res.cause().getMessage());
            }
        });
    }

    private void updateProduct(RoutingContext context) {
        String id = context.pathParam("id");
        JsonObject update = context.getBodyAsJson();
        mongoClient.findOneAndUpdate(productCollection, new JsonObject().put("_id", id), new JsonObject().put("$set", update), res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    context.response()
                            .setStatusCode(200)
                            .end(res.result().encode());
                } else {
                    context.response()
                            .setStatusCode(404)
                            .end();
                }
            } else {
                context.response()
                        .setStatusCode(500)
                        .end(res.cause().getMessage());
            }
        });
    }

    @Override
    public void stop() {
        mongoClient.close();
    }
}