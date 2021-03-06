package engine;

import game.Board;
import game.Move;
import game.Piece;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;

public class ZobristHashCalculator {

    private final long[][][] positionValue;

    private final static int WHITE = 0;
    private final static int BLACK = 1;

    public ZobristHashCalculator(int rows, int columns) {
        positionValue = new long[rows][columns][2];
        fillTables();
    }

    private static int getPieceValue(Piece piece) {
        if (piece.isBlack()) {
            return BLACK;
        } else {
            return WHITE;
        }
    }

    private void fillTables() {
        HashSet<Long> uniqueRandoms = new HashSet<>();
        SecureRandom random = new SecureRandom();
        for (int y = 0; y < positionValue.length; y++) {
            for (int x = 0; x < positionValue[y].length; x++) {
                for (int i = 0; i < positionValue[y][x].length; i++) {
                    boolean unique = false;
                    while (!unique) {
                        long randomHashValue = random.nextLong();
                        unique = uniqueRandoms.add(randomHashValue);
                        if (unique) {
                            positionValue[y][x][i] = randomHashValue;
                        }
                    }
                }
            }
        }
    }

    public long calculateHash(Board board) {
        long hash = 0;
        for (int x = 0; x < board.getRows(); x++) {
            for (int y = 0; y < board.getCols(); y++) {
                Piece piece = board.getPieceAtPosition(x, y);
                if (piece != null) {
                    hash ^= positionValue[x][y][getPieceValue(piece)];
                }
            }
        }
        return hash;
    }

    public long updateHash(long hash, Move move, Piece piece) {
        // Not a null move
        if (move != null && piece != null) {
            int pieceValue = getPieceValue(piece);
            Piece takenOpponentsPiece = move.getCapturedPiece();
            if (takenOpponentsPiece != null) {
                hash ^= positionValue[takenOpponentsPiece.getRow()][takenOpponentsPiece.getCol()][getPieceValue(takenOpponentsPiece)];
            }
            hash ^= positionValue[move.getSourceRow()][move.getSourceCol()][pieceValue];
            hash ^= positionValue[move.getTargetRow()][move.getTargetCol()][pieceValue];
        }

        return hash;
    }
}