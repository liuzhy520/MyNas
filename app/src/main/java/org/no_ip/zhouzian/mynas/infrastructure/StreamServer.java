package org.no_ip.zhouzian.mynas.infrastructure;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import android.util.Log;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

/**
 * A single-connection HTTP server that will respond to requests for files and
 * pull them from the application's SD card.
 */
public class StreamServer implements Runnable {
    private static final String TAG = StreamServer.class.getName();
    private int port = 0;
    private boolean isRunning = false;
    private ServerSocket socket;
    private Thread thread;
    private long cbSkip;
    private boolean seekRequest;
    private SmbFile smbFile;
    private String fileName;

    /**
     * This server accepts HTTP request.
     */
    public StreamServer() {}

    public void setFile (SmbFile file) {
        smbFile = file;
        fileName = smbFile.getName();
    }

    /**
     * @return A port number assigned by the OS.
     */
    public int getPort() {
        return port;
    }

    /**
     * Prepare the server to start.
     * <p/>
     * This only needs to be called once per instance. Once initialized, the
     * server can be started and stopped as needed.
     */
    public String init(String ip) {
        String url = null;
        try {
            InetAddress inet = InetAddress.getByName(ip);
            byte[] bytes = inet.getAddress();
            socket = new ServerSocket(port, 0, InetAddress.getByAddress(bytes));

            socket.setSoTimeout(10000);
            port = socket.getLocalPort();
            url = "http://" + socket.getInetAddress().getHostAddress() + ":"
                    + port;
            Log.e(TAG, "Server started at " + url);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error UnknownHostException server", e);
        } catch (IOException e) {
            Log.e(TAG, "Error IOException server", e);
        }
        return url;
    }

    public String getFileUrl() {
        return "http://" + socket.getInetAddress().getHostAddress() + ":"
                + port + "/" + fileName;
    }

    /**
     * Start the server.
     */
    public void start() {
        thread = new Thread(this);
        thread.start();
        isRunning = true;
    }

    /**
     * Stop the server.
     * <p/>
     * This stops the thread listening to the port. It may take up to five
     * seconds to close the service and this call blocks until that occurs.
     */
    public void stop() {
        isRunning = false;
        if (thread == null) {
            Log.e(TAG, "Server was stopped without being started.");
            return;
        }
        Log.e(TAG, "Stopping server.");
        thread.interrupt();
    }

    /**
     * Determines if the server is running (i.e. has been <code>start</code>ed
     * and has not been <code>stop</code>ed.
     *
     * @return <code>true</code> if the server is running, otherwise
     * <code>false</code>
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * This is used internally by the server and should not be called directly.
     */
    @Override
    public void run() {
        Log.e(TAG, "running");
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null) {
                    continue;
                }
                Log.e(TAG, "client connected at " + port);
                ExternalResourceDataSource data = new ExternalResourceDataSource(
                        smbFile);
                Log.e(TAG, "processing request...");
                processRequest(data, client);
            } catch (SocketTimeoutException e) {
                //Log.e(TAG, "No client connected, waiting for client...", e);
                // Do nothing
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to client", e);
                // break;
            }
        }
        Log.e(TAG, "Server interrupted or stopped. Shutting down.");
    }

    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     */
    private int findHeaderEnd(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 3 < rlen) {
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n'
                    && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n')
                return splitbyte + 4;
            splitbyte++;
        }
        return 0;
    }

    /*
     * Sends the HTTP response to the client, including headers (as applicable)
     * and content.
     */
    private void processRequest(ExternalResourceDataSource dataSource,
                                Socket client) throws IllegalStateException, IOException {
        if (dataSource == null) {
            Log.e(TAG, "Invalid (null) resource.");
            client.close();
            return;
        }
        InputStream is = client.getInputStream();
        final int bufsize = 8192;
        byte[] buf = new byte[bufsize];
        int splitbyte = 0;
        int rlen = 0;
        {
            int read = is.read(buf, 0, bufsize);
            while (read > 0) {
                rlen += read;
                splitbyte = findHeaderEnd(buf, rlen);
                if (splitbyte > 0)
                    break;
                read = is.read(buf, rlen, bufsize - rlen);
            }
        }

        // Create a BufferedReader for parsing the header.
        ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
        BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
        Properties pre = new Properties();
        Properties parms = new Properties();
        Properties header = new Properties();

        try {
            decodeHeader(hin, pre, parms, header);
        } catch (InterruptedException e1) {
            Log.e(TAG, "Exception: " + e1.getMessage());
            e1.printStackTrace();
        }
        for (Entry<Object, Object> e : header.entrySet()) {
            Log.e(TAG, "Header: " + e.getKey() + " : " + e.getValue());
        }
        String range = header.getProperty("range");
        cbSkip = 0;
        //seekRequest = false;
        if (range != null) {
            Log.e(TAG, "range is: " + range);
            //seekRequest = true;
            range = range.substring(6);
            int charPos = range.indexOf('-');
            if (charPos > 0) {
                range = range.substring(0, charPos);
            }
            cbSkip = Long.parseLong(range);
            Log.e(TAG, "range found!! " + cbSkip);
        }
        String headers = "";
        headers += "HTTP/1.1 206 Partial Content\r\n";
        headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
        headers += "Accept-Ranges: bytes\r\n";
        headers += "Content-Length: " + dataSource.getContentLength()
                + "\r\n";
        headers += "Content-Range: bytes " + cbSkip + "-"
                + dataSource.getContentLength() + "/*\r\n";
        headers += "\r\n";

        InputStream data = null;
        try {
            data = dataSource.createInputStream();
            byte[] buffer = headers.getBytes();
            Log.e(TAG, "writing to client");
            client.getOutputStream().write(buffer, 0, buffer.length);

            // Start sending content.

            byte[] buff = new byte[1024 * 8];
            Log.e(TAG, "No of bytes skipped: " + data.skip(cbSkip));
            int cbSentThisBatch = 0;
            while (isRunning) {
                int cbRead = data.read(buff, 0, buff.length);
                if (cbRead == -1) {
                    /*Log.e(TAG,
                            "readybytes are -1 and this is simulate streaming, close the ips and crate anotber  ");
                    data.close();
                    data = dataSource.createInputStream();
                    cbRead = data.read(buff, 0, buff.length);
                    if (cbRead == -1) {
                        Log.e(TAG, "error in reading bytess**********");
                        throw new IOException(
                                "Error re-opening data source for looping.");
                    }*/
                    data.close();
                    break;
                }
                client.getOutputStream().write(buff, 0, cbRead);
                client.getOutputStream().flush();
                cbSkip += cbRead;
                cbSentThisBatch += cbRead;
            }
            Log.e(TAG, "cbSentThisBatch: " + cbSentThisBatch);
            // If we did nothing this batch, block for a second
            if (cbSentThisBatch == 0) {
                Log.e(TAG, "Blocking until more data appears");
                Thread.sleep(1000);
            }
        } catch (SocketException e) {
            // Ignore when the client breaks connection
            Log.e(TAG, "Ignoring " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error getting content stream.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error streaming file content.", e);
        } finally {
            if (data != null) {
                data.close();
                Log.e(TAG, "Smb input stream was properly closed!");
            }
            client.close();
        }
    }

    /**
     * Decodes the sent headers and loads the data into java Properties' key -
     * value pairs
     */
    private void decodeHeader(BufferedReader in, Properties pre,
                              Properties parms, Properties header) throws InterruptedException {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null)
                return;
            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

            String method = st.nextToken();
            pre.put("method", method);

            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = decodePercent(uri.substring(0, qmi));
            } else
                uri = decodePercent(uri);

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0)
                        header.put(line.substring(0, p).trim().toLowerCase(),
                                line.substring(p + 1).trim());
                    line = in.readLine();
                }
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            Log.e(TAG,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        }
    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
     * Properties. NOTE: this doesn't support multiple identical keys due to the
     * simplicity of Properties -- if you need multiples, you might want to
     * replace the Properties with a Hashtable of Vectors or such.
     */
    private void decodeParms(String parms, Properties p)
            throws InterruptedException {
        if (parms == null)
            return;

        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0)
                p.put(decodePercent(e.substring(0, sep)).trim(),
                        decodePercent(e.substring(sep + 1)));
        }
    }

    /**
     * Decodes the percent encoding scheme. <br/>
     * For example: "an+example%20string" -> "an example string"
     */
    private String decodePercent(String str) throws InterruptedException {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        break;
                    case '%':
                        sb.append((char) Integer.parseInt(
                                str.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "BAD REQUEST: Bad percent-encoding.");
            return null;
        }
    }

    /**
     * provides meta-data and access to a stream for resources on SD card.
     */
    protected class ExternalResourceDataSource {

        private InputStream inputStream;
        private SmbFile smbResource;
        long contentLength;

        public ExternalResourceDataSource(SmbFile resource) {
            smbResource = resource;
            try {
                contentLength = resource.length();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //Log.e(TAG, "respurcePath is: " + smbFile.getPath());
        }

        /**
         * Returns a MIME-compatible content type (e.g. "text/html") for the
         * resource. This method must be implemented.
         *
         * @return A MIME content type.
         */
        public String getContentType() {
            String extension = "";

            int i = smbResource.getName().lastIndexOf('.');
            if (i > 0) {
                extension = smbResource.getName().substring(i + 1);
            }
            switch (extension.toLowerCase()) {
                case "mp3": return "audio/x-wav";
                default: return "";
            }
        }

        /**
         * Creates and opens an input stream that returns the contents of the
         * resource. This method must be implemented.
         *
         * @return An <code>InputStream</code> to access the resource.
         * @throws IOException If the implementing class produces an error when opening
         *                     the stream.
         */
        public InputStream createInputStream() throws IOException {
            // NB: Because createInputStream can only be called once per asset
            // we always create a new file descriptor here.
            getInputStream();
            return inputStream;
        }

        /**
         * Returns the length of resource in bytes.
         * <p/>
         * By default this returns -1, which causes no content-type header to be
         * sent to the client. This would make sense for a stream content of
         * unknown or undefined length. If your resource has a defined length
         * you should override this method and return that.
         *
         * @return The length of the resource in bytes.
         */
        public long getContentLength() {
            return contentLength;
        }

        private void getInputStream() {
            try {
                inputStream = smbResource.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

}