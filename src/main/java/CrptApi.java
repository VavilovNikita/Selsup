import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {

    private final String baseUrl;
    private final int requestLimit;
    private final long timeIntervalMillis;
    private final AtomicInteger requestCount;
    private final Object lock = new Object();

    public CrptApi(String baseUrl, TimeUnit timeUnit, int requestLimit) {
        this.baseUrl = baseUrl;
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        this.requestCount = new AtomicInteger(0);
    }

    public void createDocument(Object document) throws InterruptedException {
        waitForRequests();
        ObjectMapper objectMapper = new ObjectMapper();

        HttpURLConnection connection = null;

        try {
            URL url = new URL(baseUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            String jsonString = objectMapper.writeValueAsString(document);

            OutputStream outputStream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

            writer.write(jsonString);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            incrementRequestCount();
        }
    }


    private void waitForRequests() throws InterruptedException {
        synchronized (lock) {
            while (requestCount.get() >= requestLimit) {
                lock.wait();
            }
        }
    }

    private void incrementRequestCount() {
        synchronized (lock) {
            requestCount.incrementAndGet();
            lock.notifyAll();
        }
        try {
            Thread.sleep(timeIntervalMillis / requestLimit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            requestCount.decrementAndGet();
            lock.notifyAll();
        }
    }
}
