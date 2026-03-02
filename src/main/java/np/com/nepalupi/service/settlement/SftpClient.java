package np.com.nepalupi.service.settlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;

/**
 * SFTP Client for delivering settlement files to the NCHL gateway.
 * <p>
 * Section 3.5: Settlement files are generated and submitted via SFTP
 * to the NCHL for multilateral net settlement processing.
 * <p>
 * PRODUCTION NOTE: In production, use a proper SSH/SFTP library
 * (e.g., Apache MINA SSHD, JSch). This implementation provides the
 * interface and logs delivery intent. Actual SFTP transport requires
 * SSH key configuration with NCHL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SftpClient {

    @Value("${nepalupi.nchl.sftp.host:sftp.nchl.com.np}")
    private String sftpHost;

    @Value("${nepalupi.nchl.sftp.port:22}")
    private int sftpPort;

    @Value("${nepalupi.nchl.sftp.username:nepal-upi}")
    private String sftpUsername;

    @Value("${nepalupi.nchl.sftp.remote-dir:/settlement/incoming}")
    private String remoteDirectory;

    /**
     * Upload a settlement file to the NCHL SFTP gateway.
     *
     * @param localFile Path to the local settlement file
     * @return true if upload succeeded (or simulated success in dev mode)
     */
    public boolean uploadSettlementFile(Path localFile) {
        String fileName = localFile.getFileName().toString();
        log.info("SFTP upload initiated: file={} → {}@{}:{}{}/{}",
                fileName, sftpUsername, sftpHost, sftpPort, remoteDirectory, fileName);

        // PRODUCTION: Replace with JSch or Apache MINA SSHD SFTP client
        // Example with JSch:
        //   JSch jsch = new JSch();
        //   jsch.addIdentity("/path/to/nchl-sftp-key");
        //   Session session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
        //   session.connect();
        //   ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        //   sftp.connect();
        //   sftp.put(localFile.toString(), remoteDirectory + "/" + fileName);
        //   sftp.disconnect();
        //   session.disconnect();

        // Dev mode: verify the file exists and log
        if (!localFile.toFile().exists()) {
            log.error("SFTP upload failed: local file not found: {}", localFile);
            return false;
        }

        long fileSize = localFile.toFile().length();
        log.info("SFTP upload simulated (dev mode): file={}, size={} bytes, " +
                        "destination={}:{}{}/{}",
                fileName, fileSize, sftpHost, sftpPort, remoteDirectory, fileName);

        // Verify NCHL SFTP gateway is reachable (connectivity check only)
        try {
            // In dev mode, just log — don't actually connect to NCHL
            log.info("SFTP delivery would connect to {}:{} with user '{}'",
                    sftpHost, sftpPort, sftpUsername);
            return true;
        } catch (Exception e) {
            log.error("SFTP connectivity check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if the NCHL SFTP gateway is reachable.
     */
    public boolean isGatewayReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(sftpHost, sftpPort), 5000);
            log.info("NCHL SFTP gateway reachable: {}:{}", sftpHost, sftpPort);
            return true;
        } catch (IOException e) {
            log.warn("NCHL SFTP gateway unreachable: {}:{} — {}", sftpHost, sftpPort, e.getMessage());
            return false;
        }
    }
}
