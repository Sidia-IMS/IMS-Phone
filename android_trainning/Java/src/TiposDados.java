public class TiposDados {

    public static void main(String[] args) {

        Double d = 12685495269452345.4123413247123984;
        Float f = 8768217.12736123f;
        Long l = 2341234l;
        Integer i = 200;
        Short s = 12;
        Byte b = 20;
        Boolean bool = false;
        Character c = 'C';
        String str = "string";

        System.out.println("Double: Max: " + Double.MAX_VALUE + " - Double: " + Double.MIN_VALUE);
        System.out.println("Float: Max: " + Float.MAX_VALUE + " - Min: " + Float.MIN_VALUE);
        System.out.println("Long: Max: " + Long.MAX_VALUE + " - Long: " + Long.MIN_VALUE);
        System.out.println("Integer: Max: " + Integer.MAX_VALUE + " - Min: " + Integer.MIN_VALUE);
        System.out.println("Short: Max: " + Short.MAX_VALUE + " - Min: " + Short.MIN_VALUE);
        System.out.println("Byte: Max: " + Byte.MAX_VALUE + " - Min: " + Byte.MIN_VALUE);

    }

}