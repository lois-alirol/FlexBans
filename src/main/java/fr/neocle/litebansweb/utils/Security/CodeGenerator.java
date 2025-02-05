package fr.neocle.litebansweb.utils.Security;

public class CodeGenerator {
    public static String generateCode() {
        String characters = "0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }
}