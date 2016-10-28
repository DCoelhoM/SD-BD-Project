javac -Xlint iBei/Auxiliar/TextFile.java
javac -Xlint iBei/Auxiliar/ObjectFile.java
javac -Xlint iBei/Auxiliar/Auction.java
javac -Xlint iBei/Auxiliar/User.java
javac -Xlint iBei/Auxiliar/DataTransfer.java

javac -Xlint iBei/RMIServer/RMIServer.java
javac -Xlint iBei/TCPServer/TCPServer.java

javac -Xlint iBei/RMIServer/RMIServerImpl.java

javac -Xlint iBei/TCPServer/UDPSender.java
javac -Xlint iBei/TCPServer/TCPServerImpl.java

javac -Xlint iBei/Admin/Admin.java


jar cvfm RMI.jar MANIFEST_RMI iBei/Auxiliar/*.class iBei/TCPServer/TCPServer.class iBei/RMIServer/*.class

jar cvfm TCP.jar MANIFEST_TCP iBei/Auxiliar/Auction.class iBei/Auxiliar/DataTransfer.class iBei/RMIServer/RMIServer.class iBei/TCPServer/*.class

jar cvfm ADMIN.jar MANIFEST_ADMIN iBei/Auxiliar/Auction.class iBei/Auxiliar/DataTransfer.class iBei/RMIServer/RMIServer.class iBei/TCPServer/TCPServer.class iBei/Admin/*.class
