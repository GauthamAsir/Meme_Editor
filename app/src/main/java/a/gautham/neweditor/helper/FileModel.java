package a.gautham.neweditor.helper;

import java.io.File;

public class FileModel {

    private String name;
    private String size;
    private File file;

    public FileModel(String name, String size, File file) {
        this.name = name;
        this.size = size;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
