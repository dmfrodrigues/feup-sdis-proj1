package sdis.Utils;

public class Utils {
    static public int find_nth(byte[] haystack, byte[] needle, int n){
        for(int i = 0; i+needle.length <= haystack.length; ++i){
            boolean occurs = true;
            for(int j = 0; j < needle.length; ++j){
                if(haystack[i+j] != needle[j]){
                    occurs = false;
                    break;
                }
            }
            if(occurs) --n;
            if(n == 0) return i;
        }
        return -1;
    }

    /**
     * Convert array of bytes to string with hexadecimal representation.
     *
     * @param array     Array of bytes to convert
     * @return          String with hexadecimal conversion
     */
    public static String bytesToHexString(byte[] array) {
        StringBuilder hexString = new StringBuilder(2*array.length);
        for (byte b : array) {
            String hex = Integer.toHexString(0xff & b).toUpperCase();
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
