package test

/**
 * Created by Zawad on 3/16/2017.
 */
class CsvFileWriter {
    private static final String COMMA_DELIMTER = ","
    private static final String NEW_LINE_SEPARATOR = "\n"

    private String FILE_HEADER = ""
    private File file = null


    public CsvFileWriter(String path){
             file = new File(path)
        if(file.exists()){
            file.delete()
            file = new File(path)
        }
    }

    public void writeToCSVFile(String...values){

        for(String writeData:values){
            try{
                file.append(writeData)
                file.append(COMMA_DELIMTER)
            }catch (Exception e){
                println("Error in writing to the File")
                   e.printStackTrace()
            }
        }

        file.append(NEW_LINE_SEPARATOR)

    }

    public void writeToCSVFile(String value){
        try{
        file.append(value)
        }catch (Exception e){
            println("Error in writing to the File")
            e.printStackTrace()
        }

        file.append(NEW_LINE_SEPARATOR)
    }

    public void setFileHeader(String header){
        FILE_HEADER=header
    }

    public String getFileHeader(){
        return FILE_HEADER
    }

    public void writeFileHeader(){
        file.append(FILE_HEADER.toString())
        file.append(NEW_LINE_SEPARATOR)

    }
}
