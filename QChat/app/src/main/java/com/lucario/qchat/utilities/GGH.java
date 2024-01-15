package com.lucario.qchat.utilities;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

public class GGH {
    private RealMatrix privateBasis;
    private RealMatrix publicBasis;
    private RealMatrix unimodular;

    public String getPrivateBasis(){
        return encodeToBase64(privateBasis);
    }

    public String getPublicBasis(){
        return encodeToBase64(publicBasis);
    }

    public String getBadVector(){
        return encodeToBase64(unimodular);
    }

    public GGH(int n, long seed) {
        privateBasis = MatrixUtils.createRealIdentityMatrix(n);
        RandomDataGenerator random = new RandomDataGenerator();
        random.reSeed(seed);

        while (true) {
            unimodular = randUnimod(n, random);
            RealMatrix temp = privateBasis.multiply(unimodular);
            double ratio = hammingRatio(temp, n);
            if (ratio <= .1) {
                publicBasis = temp;
                break;
            }
        }
    }

    public RealMatrix encrypt(byte[] message) {
        // Assume message is 16 bytes
        double[][] keyMatrixData = new double[4][4];
        for (int i = 0; i < message.length; i++) {
            keyMatrixData[i / 4][i % 4] = message[i];
        }
        RealMatrix keyMatrix = MatrixUtils.createRealMatrix(keyMatrixData);

        // Pad the key matrix to fit into a 10x10 matrix for encryption
        RealMatrix paddedKeyMatrix = MatrixUtils.createRealMatrix(10, 10);
        paddedKeyMatrix.setSubMatrix(keyMatrix.getData(), 0, 0);

        return paddedKeyMatrix.multiply(publicBasis);
    }

    public static String encrypt(byte [] message, String publicKey) {
        // Assume message is 16 bytes
        RealMatrix publicBasis = decodeFromBase64(publicKey,10,10);
        double[][] keyMatrixData = new double[4][4];
        for (int i = 0; i < message.length; i++) {
            keyMatrixData[i / 4][i % 4] = message[i];
        }
        RealMatrix keyMatrix = MatrixUtils.createRealMatrix(keyMatrixData);

        // Pad the key matrix to fit into a 10x10 matrix for encryption
        RealMatrix paddedKeyMatrix = MatrixUtils.createRealMatrix(10, 10);
        paddedKeyMatrix.setSubMatrix(keyMatrix.getData(), 0, 0);

        return encodeToBase64(paddedKeyMatrix.multiply(publicBasis));
    }

    public static byte[] decrypt(RealMatrix encrypted, String privateKey, String badVector) {
        RealMatrix unimodular = decodeFromBase64(badVector,10,10);
        RealMatrix privateBasis = decodeFromBase64(privateKey,10,10);
        RealMatrix bPrime = new LUDecomposition(privateBasis).getSolver().getInverse();
        RealMatrix bb = bPrime.multiply(encrypted);
        RealMatrix unimodularInverse = new LUDecomposition(unimodular).getSolver().getInverse();
        RealMatrix m = bb.multiply(unimodularInverse);

        // Since there is no direct rounding function on matrices in Commons Math, we have to do it manually
        double[][] data = m.getData();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = Math.round(data[i][j]);
            }
        }
        RealMatrix roundedMatrix = MatrixUtils.createRealMatrix(data);

        RealMatrix originalMatrix = roundedMatrix.getSubMatrix(0, 3, 0, 3);
        return matrixToByteArray(originalMatrix);
    }

    private static RealMatrix randUnimod(int n, RandomDataGenerator random) {
        RealMatrix upperTri = MatrixUtils.createRealMatrix(n, n);
        RealMatrix lowerTri = MatrixUtils.createRealIdentityMatrix(n);

        for (int i = 0; i < n; ++i) {
            for (int j = i; j < n; ++j) {
                upperTri.setEntry(i, j, random.nextInt(-10, 10));
            }
            for (int j = 0; j < i; ++j) {
                lowerTri.setEntry(i, j, random.nextInt(-10, 10));
            }
            int diagVal = random.nextSecureInt(0, 1) * 2 - 1;
            upperTri.setEntry(i, i, diagVal);
            lowerTri.setEntry(i, i, diagVal);
        }
        return upperTri.multiply(lowerTri);
    }

    private static double hammingRatio(RealMatrix basis, int n) {
        double detB = new LUDecomposition(basis).getDeterminant();
        double mult = 1;
        for (int i = 0; i < basis.getRowDimension(); i++) {
            mult *= new ArrayRealVector(basis.getRow(i)).getNorm();
        }

        return Math.pow(detB / mult, 1.0 / n);
    }

    private static byte[] matrixToByteArray(RealMatrix matrix) {
        int rows = matrix.getRowDimension();
        int columns = matrix.getColumnDimension();
        byte[] byteArray = new byte[rows * columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                byteArray[i * columns + j] = (byte) matrix.getEntry(i, j);
            }
        }

        return byteArray;
    }



//    public static void main(String[] args) {
//        int dimension = 10;
//        GGH ggh = new GGH(dimension,123);
//
//        byte[] aesKey = "à¤øèçæåä".getBytes();
//
//        RealMatrix encrypted = ggh.encrypt(aesKey);
//        String base64en = encodeToBase64(encrypted);
//        RealMatrix ag = decodeFromBase64(base64en,10,10);
//        byte[] decrypted = ggh.decrypt(ag, );
//
//        System.out.println("Original key: " + new String(aesKey));
//        System.out.println("Decrypted key: " + new String(decrypted));
//    }

    private static byte[] doubleMatrixToByteArray(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        // Using ByteBuffer to convert double values to bytes
        ByteBuffer buffer = ByteBuffer.allocate(rows * cols * Double.BYTES);

        for (double[] doubles : matrix) {
            for (int j = 0; j < cols; j++) {
                buffer.putDouble(doubles[j]);
            }
        }

        return buffer.array();
    }

    public static String encodeToBase64(RealMatrix realMatrix) {
        // Using Java 8's Base64 class for encoding
        return Base64.getEncoder().encodeToString(doubleMatrixToByteArray(realMatrix.getData()));
    }

    private static byte[] base64ToByteArray(String base64String) {
        // Decode the base64 encoded string to a byte array
        return Base64.getDecoder().decode(base64String);
    }

    private static double[][] byteArrayToDoubleMatrix(byte[] byteArray, int rows, int cols) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        double[][] matrix = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = buffer.getDouble();
            }
        }

        return matrix;
    }

    public static RealMatrix decodeFromBase64(String base64String, int rows, int cols) {
        byte[] byteArray = base64ToByteArray(base64String);
        double[][] matrix = byteArrayToDoubleMatrix(byteArray, rows, cols);
        return new Array2DRowRealMatrix(matrix);
    }

}