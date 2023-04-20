public class Test {

    public static void main(String[] args) {
        String str = ".My name is Mahadi";


        String last = str.substring(str.lastIndexOf(" ")+1);


        str =  str.replaceAll(last, " "+last+" Sekh");
        str = str.replaceFirst("\\.", "");
        System.out.println(last);
        System.out.println(str);
    }
}
