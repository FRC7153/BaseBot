package frc.team7153.AutoRecorder;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class HTTPHandler extends SubsystemBase {
    // Init
    private HttpClient client = HttpClient.newHttpClient();
    private Logger log;

    public String payloadId;
    private String ip;
    public boolean error = false;
    public boolean sentDone = false;

    // Payload
    private String payload;
    private int sizePerPacket;
    public int numOfPackets;
    public int packetsSent = 0;

    public boolean doneSendingPackets() { return (packetsSent == numOfPackets); }

    // Constructor
    private void init(String ipAddress, int size) {
        ip = ipAddress;
        log = Logger.getLogger("Auto HTTP Handler");
        sizePerPacket = size;

        if (size < 10) {
            log.warning(String.format("[Non-Fatal] Should not send packets less than 10 characters! (set to %s)", size));
        }
    }

    public HTTPHandler(String ipAddress) { init(ipAddress, 1000); }
    public HTTPHandler(String ipAddress, int sizePerPacket) { init(ipAddress, sizePerPacket); }

    // Success Message (Will Also Format Strings) (Will NOT Log if 'error' is True)
    private void logSuccess(String  msg, Object... args) {
        if (error) { return; }
        log.log(Level.INFO, String.format("[SUCCESS] " + msg, args));
    }

    // Send HTTP POST Request
    private String sendPOST(String path, Object content, String requestIdentifier, Boolean preventRecursive) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(String.format("http://%s:5000%s", ip, path)))
            .header("accept", "text/plain; charset=UTF-8")
            .header("Content-Type", "text/plain; charset=UTF-8")
            .POST(BodyPublishers.ofString(String.valueOf(content)))
            .build();
        
        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new Exception(String.format("Status Code %s: Message: %s", resp.statusCode(), resp.body()));
            }
            return resp.body();
        } catch (Exception e) {
            log.warning(String.format("Error while sending HTTP POST request (id: '%s'): '%s'", requestIdentifier, e));
            error = true;

            if (!preventRecursive) { terminatePayload(false); } // to prevent downed server from causing infinte number of requests

            return "";
        }
    }

    private String sendPOST(String path, Object content, String requestIdentifier) { return sendPOST(path, content, requestIdentifier, false); }

    // Register a New Payload
    public void initiatePayload(String fullPayload) {
        payload = fullPayload;
        numOfPackets = (int)Math.ceil(((double)payload.length())/((double)sizePerPacket));

        payloadId = sendPOST("/send/newPayload", numOfPackets, "initiate payload request");
        logSuccess("Initiated new payload, id '%s'", payloadId);
    }

    // Send Payload Title
    public void sendPayloadTitle(String title) {
        sendPOST("/send/title?id="+payloadId, title, "payload title request");
        logSuccess("Sent payload title '%s' for id '%s'", title, payloadId);
    }

    // Send Packet
    public void sendNextPacket() {
        if (doneSendingPackets()) {
            log.warning("sendNextPacket called, but already finished sending packets! (skipped)");
            return;
        }

        packetsSent += 1;
        String packet = payload.substring((packetsSent-1)*sizePerPacket, Math.min(packetsSent*sizePerPacket, payload.length()));
        String packetSentStr = String.valueOf(packetsSent);

        sendPOST("/send/packet?id="+payloadId+"&packetNum="+packetSentStr, packet, "packet transfer "+packetSentStr);
        logSuccess("Sent packet %s/%s", packetsSent, numOfPackets);
    }

    // Close Payload Request
    public void terminatePayload(Boolean success) {
        sendPOST("/send/terminate?id="+payloadId, (success) ? 1 : 0, String.format("terminate request (success: %s)", success), true);
        logSuccess("Terminated payload request (id: %s, success: %s)", payloadId, success);
        sentDone =  true;
    }
}
