package ntu.mdp.grp18;

public class MapDecoder {
    // 0: unexplored
    // 1: explored
    // 2: blank
    // 3: obstacle
    // 4: robot position
    // state will not be in 1

    private static int[] flatMapArray = new int[300];


    public static void clearFlatMapArray(){
        flatMapArray = new int[300];
    }



    public static int[][] convertToMap(String exploredHex, String obstacleHex){
        hexToExploredMap(exploredHex);
        // there will be explored but unassigned value in flatMapArray now
        hexToObstacleMap(obstacleHex);
        // All explored value will be assigned a value
        return twoDConversion(flatMapArray);
    }

    private static int[][] twoDConversion(int[] flatMapArray) {
        int[][] result = new int[20][15];
        for(int i = 0; i<300; i++){
            int r = 19-i/15;
            int c = i%15;
            result[r][c] = flatMapArray[i];
        }
        return result;
    }

    private static void hexToExploredMap(String exploredHex){
        int mapPtr = 0;
        String binString = hexStringToBinString(exploredHex);
        for(int i = 2; i<binString.length()-2; i++) {
            char c = binString.charAt(i);
            if (flatMapArray[mapPtr] == 0) {
                flatMapArray[mapPtr] = c - '0';
            }
            mapPtr++;
        }
    }

    private static void hexToObstacleMap(String obstacleHex){
        String binString = hexStringToBinString(obstacleHex);
        int binStringPtr = 0;
        for(int i = 0; i<300; i++){
            // if still unexplored, continue without incrementing binStringPtr
            if(flatMapArray[i]==0) continue;
            // if not unexplored, definitely need check with binString
            else if(flatMapArray[i]==1){ // if explored but not assigned with value, assign it
                // if obstacleString is 1, means there is obstacle, indicate with 3, else 2
                flatMapArray[i] = (binString.charAt(binStringPtr)=='1') ? 3 : 2;
            }
            binStringPtr++;
        }
    }

    private static String hexStringToBinString(String hexString){
        StringBuilder sb = new StringBuilder();
        for(char c: hexString.toCharArray()){
            sb.append(hex_bin_converter(String.valueOf(c)));
        }
        return sb.toString();
    }

    private static String hex_bin_converter(String hex){
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        switch (bin.length()){
            case 1:
                bin = "000"+bin;
                break;
            case 2:
                bin = "00"+bin;
                break;
            case 3:
                bin = "0"+bin;
                break;
        }
        return bin;
    }
}
