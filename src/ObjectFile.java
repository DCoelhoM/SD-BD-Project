import java.io.*;

public class ObjectFile {
    private ObjectInputStream iS;
    private ObjectOutputStream oS;

    public void openRead(String filename) throws IOException {
        iS = new ObjectInputStream(new FileInputStream(filename));
    }

    public void openWrite(String filename) throws IOException {
        oS = new ObjectOutputStream(new FileOutputStream(filename, false));
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return iS.readObject();
    }

    public void writeObject(Object o) throws IOException {
        oS.writeObject(o);
    }

    public void closeRead() throws IOException {
        iS.close();
    }

    public void closeWrite() throws IOException {
        oS.close();
    }
}