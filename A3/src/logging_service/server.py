import os, sys, io, selectors
import socket

# A very simple high-level socket wrapper.
class SimpleSocket(socket.socket):
    def __init__(self, family=socket.AF_INET, type=socket.SOCK_STREAM, proto=0, fileno=None):
        super().__init__(family, type, proto, fileno)

    # Support using the socket in a "with" block.
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    # A simple, informative string representation.
    def __repr__(self):
        try:
            fd = self.fileno()
        except socket.error:
            fd = -1
        return (f"<SimpleSocket fd={fd} family={self.family} "
                f"type={self.type} proto={self.proto}>")

# Create a client connection to the given (host, port).
def create_connection(address, timeout=None):
    host, port = address
    last_err = None
    for res in socket.getaddrinfo(host, port, socket.AF_UNSPEC, socket.SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        try:
            s = SimpleSocket(af, socktype, proto)
            if timeout is not None:
                s.settimeout(timeout)
            s.connect(sa)
            return s  # Return the connected socket.
        except OSError as e:
            last_err = e
            s.close()
    if last_err:
        raise last_err
    raise OSError("getaddrinfo returned an empty list")

# Create a server (listening) socket bound to the given address.
def create_server(address, backlog=5):
    s = SimpleSocket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(address)
    s.listen(backlog)
    return s

# Example usage of the simplified API.
if __name__ == "__main__":
    # Create a server listening on localhost at an available port.
    server = create_server(('localhost', 0))
    print("Server listening on", server.getsockname())

    server.close()