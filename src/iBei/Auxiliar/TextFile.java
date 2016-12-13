package iBei.src.iBei.Auxiliar;
import java.io.*;

public class TextFile {

    private BufferedReader fR;
    private BufferedWriter fW;


    public void openRead(String filename) throws IOException {
        fR = new BufferedReader(new FileReader(filename));
    }

    public void openWrite(String filename) throws IOException {
        fW = new BufferedWriter(new FileWriter(filename,true));
    }

    public void openWriteOW(String filename) throws IOException {
        fW = new BufferedWriter(new FileWriter(filename,false));
    }

    public String readLine() throws IOException {
        return fR.readLine();
    }

    public void writeLine(String line) throws IOException {
        fW.write(line);
    }

    public void closeRead() throws IOException {
        fR.close();
    }

    public void closeWrite() throws IOException {
        fW.close();
    }

}