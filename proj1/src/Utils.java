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
}
