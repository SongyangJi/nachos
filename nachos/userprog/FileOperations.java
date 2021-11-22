package nachos.userprog;

import nachos.machine.FileSystem;
import nachos.machine.OpenFile;
import nachos.threads.ThreadedKernel;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/11/21
 */
public class FileOperations {

    private static final int MAX_FILES = 16;

    private final OpenFile[] fdArray;


    private static final FileSystem fileSystem = ThreadedKernel.fileSystem;

    private FileOperations() {
        fdArray = new OpenFile[MAX_FILES];
        fdArray[0] = UserKernel.console.openForReading();
        fdArray[1] = UserKernel.console.openForWriting();
    }

    public static FileOperations newFileOperations() {
        return new FileOperations();
    }

    public int createFile(String name) {
        int fd;
        for (fd = 0; fd < fdArray.length; fd++) {
            if (fdArray[fd] == null) {
                break;
            }
        }
        if (fd == MAX_FILES) return -1;
        fdArray[fd] = fileSystem.open(name, true);
        return fd;
    }

    public int openFile(String name) {
        int fd;
        for (fd = 0; fd < fdArray.length; fd++) {
            if (fdArray[fd] == null) {
                break;
            }
        }
        if (fd == MAX_FILES) return -1;
        fdArray[fd] = fileSystem.open(name, false);
        return fd;
    }

    public int readFile(int fileDescriptor, byte[] bytes, int count) {
        OpenFile file = fdArray[fileDescriptor];
        if (file == null) return -1;
        return file.read(bytes, 0, count);
    }

    public int writeFile(int fileDescriptor, byte[] bytes, int count) {
        OpenFile file = fdArray[fileDescriptor];
        if (file == null) return -1;
        return file.write(bytes, 0, count);
    }

    public int closeFile(int fileDescriptor) {
        if (fdArray[fileDescriptor] == null) {
            return -1;
        }
        OpenFile file = fdArray[fileDescriptor];
        file.close();
        fdArray[fileDescriptor] = null;
        return 0;
    }

    public int unlinkFile(String name) {
        return fileSystem.remove(name) ? 0 : -1;
    }


}
