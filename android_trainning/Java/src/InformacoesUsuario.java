import java.util.Scanner;

public class InformacoesUsuario {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Por favor, informe um número: ");

        Integer inteiro = scanner.nextInt();
        System.out.println("Número: " + inteiro);

        System.out.println("Fim da execução.");

        Double doubleValor = scanner.nextDouble();
        Float floatValor = scanner.nextFloat();
        Long longValor = scanner.nextLong();
        Integer intValor = scanner.nextInt();
        Short shortValor = scanner.nextShort();
        Byte byteValor = scanner.nextByte();
        Boolean bool = scanner.nextBoolean();
        String string = scanner.nextLine();

    }
}