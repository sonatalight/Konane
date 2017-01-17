//package engine;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//
//import game.Board;
//import game.Move;
//
//public class CABSolverParallel extends Solver {
//
//	private final TTEntry[] tTable = new TTEntry[(int) Math.pow(2, 24)];
//	private final CGTSolver cgtSolver = new CGTSolver();
//	private final ExecutorService executor = Executors.newFixedThreadPool(4);
//
//	public OutcomeType solve(Board board) {
//		CGTValue blackOutcome = new Number(-100);
//		CGTValue whiteOutcome = new Number(100);
//
//		try {
//			List<Callable<CGTValue>> blackMoveTasks = new ArrayList<>();
//			List<Callable<CGTValue>> whiteMoveTasks = new ArrayList<>();
//
//			List<Move> blackMoves = board.getLeftOptions();
//			for (Move move : blackMoves) {
//				Callable<CGTValue> callable = () -> {
//					Board innerBoard = new Board(board);
//					innerBoard.executeMove(move);
//					return solve(innerBoard, false, new Number(-100), new Number(100), 1);
//				};
//				blackMoveTasks.add(callable);
//			}
//
//			List<Move> whiteMoves = board.getRightOptions();
//			for (Move move : whiteMoves) {
//				Callable<CGTValue> callable = () -> {
//					Board innerBoard = new Board(board);
//					innerBoard.executeMove(move);
//					return solve(innerBoard, true, new Number(-100), new Number(100), 1);
//				};
//				whiteMoveTasks.add(callable);
//			}
//
//			List<Future<CGTValue>> blackOutcomes = executor.invokeAll(blackMoveTasks);
//			List<Future<CGTValue>> whiteOutcomes = executor.invokeAll(whiteMoveTasks);
//
//			for (Future<CGTValue> singleBlackOutcome : blackOutcomes) {
//				CGTValue cgtValue = singleBlackOutcome.get();
//				blackOutcome = CGTValue.max(cgtValue, blackOutcome, true);
//			}
//
//			for (Future<CGTValue> singleWhiteOutcome : whiteOutcomes) {
//				CGTValue cgtValue = singleWhiteOutcome.get();
//				whiteOutcome = CGTValue.max(cgtValue, whiteOutcome, false);
//			}
//
//			System.out.println("AB-CGT solver:");
//			System.out.println("Node counter: " + counter);
//			System.out.println("Nodes looked up in TT: " + counterTT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			this.shutdown();
//		}
//		return determineWinner(blackOutcome, whiteOutcome);
//	}
//
//	@Override
//	public void printCounter() {
//
//	}
//
//	private CGTValue solve(Board board, boolean blackTurn, CGTValue alpha, CGTValue beta, int ply) {
//
//		// Lookup in TT
//		long boardHash = board.getZobristHash();
//		TTEntry ttEntry = tTable[getIndexOfHash(boardHash)];
//		if (ttEntry != null) {
//			if (ttEntry.getZobristHash() == boardHash) {
//				counterTT++;
//				return ttEntry.getCgtValue();
//			}
//		}
//		counter++;
//		CGTValue returnValue = null;
//		Move bestLeftOption = null;
//		Move bestRightOption = null;
//
//		List<Move> availableMoves;
//		if (blackTurn) {
//			availableMoves = board.getLeftOptions();
//		} else {
//			availableMoves = board.getRightOptions();
//		}
//
//		if (availableMoves.isEmpty()) {
//			if (blackTurn) {
//				returnValue = new Number(-1); // White wins
//			} else {
//				returnValue = new Number(1); // Black wins
//			}
//		} else {
//			orderMoves(availableMoves, board, blackTurn);
//		}
//
//		if (returnValue == null) {
//			if (board.isEndgame()) {
//				// Do CGT Stuff
//				returnValue = cgtSolver.calculate(board);
//			}
//		}
//
//		CGTValue value;
//
//		if (returnValue == null) {
//			if (blackTurn) {
//				returnValue = new Number(-Double.MAX_VALUE);
//				for (Move move : availableMoves) {
//					board.executeMove(move);
//					value = solve(board, false, alpha, beta, ply + 1);
//					board.revertMove(move);
//
//					if (CGTValue.greater(value, returnValue)) {
//						returnValue = value;
//					}
//
//					if (CGTValue.greater(returnValue, alpha)) {
//						alpha = returnValue;
//						bestLeftOption = move;
//					}
//					if (CGTValue.lessEqual(beta, alpha)) {
//						break;
//					}
//				}
//			} else {
//				returnValue = new Number(Double.MAX_VALUE);
//				for (Move move : availableMoves) {
//					board.executeMove(move);
//					value = solve(board, true, alpha, beta, ply + 1);
//					board.revertMove(move);
//
//					if (CGTValue.less(value, returnValue)) {
//						returnValue = value;
//					}
//
//					if (CGTValue.less(returnValue, beta)) {
//						beta = returnValue;
//						bestRightOption = move;
//					}
//					if (CGTValue.lessEqual(beta, alpha)) {
//						break;
//					}
//				}
//			}
//		}
//
//		tTable[getIndexOfHash(boardHash)] = new TTEntry(boardHash, bestLeftOption, bestRightOption, returnValue);
//
//		return returnValue;
//	}
//
//	public static OutcomeType determineWinner(CGTValue blackOutcome, CGTValue whiteOutcome) {
//		if (blackOutcome instanceof Number) {
//			Number black = (Number) blackOutcome;
//
//			if (whiteOutcome instanceof Number) {
//				Number white = (Number) whiteOutcome;
//
//				if (black.getValue() >= 0 && white.getValue() > 0) {
//					return OutcomeType.BLACK;
//				}
//
//				if (black.getValue() > 0 && white.getValue() < 0) {
//					return OutcomeType.FIRST;
//				}
//
//				if (black.getValue() < 0 && white.getValue() <= 0) {
//					return OutcomeType.WHITE;
//				}
//
//				if (black.getValue() < 0 && white.getValue() > 0) {
//					return OutcomeType.SECOND;
//				}
//
//				if (black.getValue() == 0 && white.getValue() == 0) {
//					return OutcomeType.FIRST;
//				}
//			} else if (whiteOutcome instanceof Nimber) {
//				// Nimber white = (Nimber)whiteOutcome;
//
//				if (black.getValue() > 0) {
//					return OutcomeType.BLACK;
//				}
//
//				if (black.getValue() < 0) {
//					return OutcomeType.SECOND;
//				}
//
//				if (black.getValue() == 0) {
//					return OutcomeType.BLACK;
//				}
//
//			} else if (whiteOutcome instanceof Switch) {
//				Switch white = (Switch) whiteOutcome;
//
//				if (black.getValue() >= 0) {
//					if (white.isNegative()) {
//						return OutcomeType.FIRST;
//					} else if (white.isPositive()) {
//						return OutcomeType.BLACK;
//					} else if (white.isNimber()) {
//						return OutcomeType.BLACK;
//					}
//				} else {
//					if (white.isNegative()) {
//						return OutcomeType.WHITE;
//					} else if (white.isPositive()) {
//						return OutcomeType.SECOND;
//					} else if (white.isNimber()) {
//						return OutcomeType.SECOND;
//					}
//				}
//
//			} else if (whiteOutcome instanceof Infinitesimal) {
//				Infinitesimal white = (Infinitesimal) whiteOutcome;
//
//				if (black.getValue() > 0) {
//					if (white.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.FIRST;
//					}
//				}
//
//				if (black.getValue() < 0) {
//					if (white.getValue() > 0) {
//						return OutcomeType.SECOND;
//					} else {
//						return OutcomeType.WHITE;
//					}
//				}
//
//				if (black.getValue() == 0) {
//					if (white.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.FIRST;
//					}
//				}
//			}
//		} else if (blackOutcome instanceof Nimber) {
//			// Nimber black = (Nimber) blackOutcome;
//
//			if (whiteOutcome instanceof Number) {
//				Number white = (Number) whiteOutcome;
//
//				if (white.getValue() > 0) {
//					return OutcomeType.SECOND;
//				}
//
//				if (white.getValue() < 0) {
//					return OutcomeType.WHITE;
//				}
//
//				if (white.getValue() == 0) {
//					return OutcomeType.WHITE;
//				}
//
//			} else if (whiteOutcome instanceof Nimber) {
//				// Nimber white = (Nimber)whiteOutcome;
//
//				return OutcomeType.SECOND;
//			} else if (whiteOutcome instanceof Switch) {
//				Switch white = (Switch) whiteOutcome;
//
//				if (white.isNegative()) {
//					return OutcomeType.WHITE;
//				} else if (white.isPositive()) {
//					return OutcomeType.SECOND;
//				} else if (white.isNimber()) {
//					return OutcomeType.SECOND;
//				}
//
//			} else if (whiteOutcome instanceof Infinitesimal) {
//				Infinitesimal white = (Infinitesimal) whiteOutcome;
//
//				if (white.getValue() > 0) {
//					return OutcomeType.SECOND;
//				} else {
//					return OutcomeType.WHITE;
//				}
//			}
//		} else if (blackOutcome instanceof Switch) {
//			Switch black = (Switch) blackOutcome;
//
//			if (whiteOutcome instanceof Number) {
//				Number white = (Number) whiteOutcome;
//
//				if (white.getValue() > 0) {
//					if (black.isNegative()) {
//						return OutcomeType.SECOND;
//					} else if (black.isPositive()) {
//						return OutcomeType.BLACK;
//					} else if (black.isNimber()) {
//						return OutcomeType.SECOND;
//					}
//				} else {
//					if (black.isNegative()) {
//						return OutcomeType.WHITE;
//					} else if (black.isPositive()) {
//						return OutcomeType.FIRST;
//					} else if (black.isNimber()) {
//						return OutcomeType.WHITE;
//					}
//				}
//
//			} else if (whiteOutcome instanceof Nimber) {
//				// Nimber white = (Nimber) whiteOutcome;
//
//				if (black.isNegative()) {
//					return OutcomeType.SECOND;
//				} else if (black.isPositive()) {
//					return OutcomeType.BLACK;
//				} else if (black.isNimber()) {
//					return OutcomeType.SECOND;
//				}
//
//			} else if (whiteOutcome instanceof Switch) {
//				Switch white = (Switch) whiteOutcome;
//
//				if (black.isNegative()) {
//					if (white.isNegative()) {
//						return OutcomeType.WHITE;
//					} else if (white.isPositive()) {
//						return OutcomeType.SECOND;
//					} else if (white.isNimber()) {
//						return OutcomeType.SECOND;
//					}
//				} else if (black.isPositive()) {
//					if (white.isNegative()) {
//						return OutcomeType.FIRST;
//					} else if (white.isPositive()) {
//						return OutcomeType.BLACK;
//					} else if (white.isNimber()) {
//						return OutcomeType.BLACK;
//					}
//				} else if (black.isNimber()) {
//					if (white.isNegative()) {
//						return OutcomeType.WHITE;
//					} else if (white.isPositive()) {
//						return OutcomeType.SECOND;
//					} else if (white.isNimber()) {
//						return OutcomeType.SECOND;
//					}
//				}
//			} else if (whiteOutcome instanceof Infinitesimal) {
//				Infinitesimal white = (Infinitesimal) whiteOutcome;
//
//				if (black.isNegative()) {
//					if (white.getValue() > 0) {
//						return OutcomeType.SECOND;
//					} else {
//						return OutcomeType.WHITE;
//					}
//				} else if (black.isPositive()) {
//					if (white.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.FIRST;
//					}
//				} else if (black.isNimber()) {
//					if (white.getValue() > 0) {
//						return OutcomeType.SECOND;
//					} else {
//						return OutcomeType.WHITE;
//					}
//				}
//			}
//		} else if (blackOutcome instanceof Infinitesimal) {
//			Infinitesimal black = (Infinitesimal) blackOutcome;
//
//			if (whiteOutcome instanceof Number) {
//				Number white = (Number) whiteOutcome;
//
//				if (white.getValue() > 0) {
//					if (black.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.SECOND;
//					}
//				}
//
//				if (white.getValue() <= 0) {
//					if (black.getValue() > 0) {
//						return OutcomeType.FIRST;
//					} else {
//						return OutcomeType.WHITE;
//					}
//				}
//			} else if (whiteOutcome instanceof Nimber) {
//				// Nimber white = (Nimber) whiteOutcome;
//
//				if (black.getValue() > 0) {
//					return OutcomeType.BLACK;
//				} else {
//					return OutcomeType.SECOND;
//				}
//			} else if (whiteOutcome instanceof Switch) {
//				Switch white = (Switch) whiteOutcome;
//
//				if (white.isNegative()) {
//					if (black.getValue() > 0) {
//						return OutcomeType.FIRST;
//					} else {
//						return OutcomeType.WHITE;
//					}
//				} else if (white.isPositive()) {
//					if (black.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.SECOND;
//					}
//				} else if (white.isNimber()) {
//					if (black.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.SECOND;
//					}
//				}
//			} else if (whiteOutcome instanceof Infinitesimal) {
//				Infinitesimal white = (Infinitesimal) whiteOutcome;
//
//				if (black.getValue() > 0) {
//					if (white.getValue() > 0) {
//						return OutcomeType.BLACK;
//					} else {
//						return OutcomeType.FIRST;
//					}
//				} else {
//					if (white.getValue() > 0) {
//						return OutcomeType.SECOND;
//					} else {
//						return OutcomeType.WHITE;
//					}
//				}
//			}
//		}
//
//		throw new IllegalArgumentException("Cannot determine winner for given arguments.");
//	}
//
//	/**
//	 * This method returns the primary hash code by using a bit mask
//	 *
//	 * @param zobristHash
//	 *            The complete hash
//	 * @return the primary hash code
//	 */
//	private int getIndexOfHash(long zobristHash) {
//		return (int) Math.abs(zobristHash & 0xFFFFFF);
//	}
//
//	public void shutdown() {
//		executor.shutdown();
//		try {
//			if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
//				executor.shutdownNow();
//			}
//		} catch (InterruptedException e1) {
//			executor.shutdownNow();
//			e1.printStackTrace();
//		}
//	}
//}
