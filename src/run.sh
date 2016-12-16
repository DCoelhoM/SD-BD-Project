javac -Xlint iBei/src/iBei/Auxiliar/TextFile.java
javac -Xlint iBei/src/iBei/Auxiliar/ObjectFile.java
javac -Xlint iBei/src/iBei/Auxiliar/Auction.java
javac -Xlint iBei/src/iBei/Auxiliar/User.java
javac -Xlint iBei/src/iBei/Auxiliar/DataTransfer.java

javac -Xlint iBei/src/iBei/RMIServer/RMIServer.java
javac -Xlint iBei/src/iBei/TCPServer/TCPServer.java

javac -Xlint iBei/src/iBei/RMIServer/RMIServerImpl.java

javac -Xlint iBei/src/iBei/TCPServer/UDPSender.java
javac -Xlint iBei/src/iBei/TCPServer/TCPServerImpl.java

javac -Xlint iBei/src/iBei/Admin/Admin.java


jar cvfm RMI.jar MANIFEST_RMI iBei/src/iBei/Auxiliar/*.class iBei/src/iBei/TCPServer/TCPServer.class iBei/src/iBei/RMIServer/*.class

jar cvfm TCP.jar MANIFEST_TCP iBei/src/iBei/Auxiliar/Auction.class iBei/src/iBei/Auxiliar/DataTransfer.class iBei/src/iBei/RMIServer/RMIServer.class iBei/src/iBei/TCPServer/*.class

jar cvfm ADMIN.jar MANIFEST_ADMIN iBei/src/iBei/Auxiliar/Auction.class iBei/src/iBei/Auxiliar/DataTransfer.class iBei/src/iBei/RMIServer/RMIServer.class iBei/src/iBei/TCPServer/TCPServer.class iBei/src/iBei/Admin/*.class
