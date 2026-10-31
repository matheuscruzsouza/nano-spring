package com.github.matheuscruzsouza.nanospring.server.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Socket wrapper that pre-populates the remote InetAddress hostname with its IP string
 * to prevent blocking Reverse DNS (PTR) lookups on virtual VPN interfaces (such as ZeroTier/Tailscale),
 * and enables TCP_NODELAY to avoid Nagle's algorithm latency on small HTTP/SSE packets.
 */
public class FastSocketWrapper extends Socket {

    private static final int DEFAULT_SOCKET_SEND_BUFFER = 256 * 1024;
    private static final int DEFAULT_SOCKET_RECV_BUFFER = 128 * 1024;

    private final Socket delegate;
    private final InetAddress fastInetAddress;

    public FastSocketWrapper(Socket delegate) {
        this.delegate = delegate;
        try {
            this.delegate.setTcpNoDelay(true);
            this.delegate.setSendBufferSize(DEFAULT_SOCKET_SEND_BUFFER);
            this.delegate.setReceiveBufferSize(DEFAULT_SOCKET_RECV_BUFFER);
        } catch (SocketException ignored) {
        }
        this.fastInetAddress = buildPreResolvedAddress(delegate.getInetAddress());
    }

    private static InetAddress buildPreResolvedAddress(InetAddress raw) {
        if (raw == null) {
            return null;
        }
        try {
            // Passing the IP string as the host parameter populates the internal hostName field
            // so getHostName() returns immediately in 0ms without querying DNS for a PTR record.
            return InetAddress.getByAddress(raw.getHostAddress(), raw.getAddress());
        } catch (UnknownHostException e) {
            return raw;
        }
    }

    @Override
    public InetAddress getInetAddress() {
        return fastInetAddress != null ? fastInetAddress : delegate.getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    public synchronized void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }

    @Override
    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return delegate.getSoTimeout();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegate.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }
}
