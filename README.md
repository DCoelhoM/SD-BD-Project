# SD-BD-Project
iBei: Reverse Auctions

Compile
```
cd src
sh run.sh
```

Create jar
```
jar cvfm ADMIN.jar MANIFEST_ADMIN .
jar cvfm RMI.jar MANIFEST_RMI .
jar cvfm TCP.jar MANIFEST_TCP .
```

Usage
```
java -jar ADMIN.jar TCP_PORT RMI_PORT
java -jar RMI.jar BACKUP_RMI_HOST BACKUP_RMI_PORT MAIN_PORT
java -jar TCP.jar TCP_HOST TCP_PORT PRIMARY_RMI_HOST PRIMARY_RMI_PORT BACKUP_RMI_HOST BACKUP_RMI_PORT
```
