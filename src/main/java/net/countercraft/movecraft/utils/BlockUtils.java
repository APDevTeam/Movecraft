/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.utils;

import java.util.Arrays;

public class BlockUtils {
	private static final int[] dataBlocks = new int[]{ 17, 51, 18, 84, 6, 81, 83, 8, 3, 59, 115, 86, 50, 66, 53, 69, 64, 77, 63, 65, 23, 70, 43, 78, 92, 26, 93, 55, 2, 96, 29, 34, 98, 99, 106, 107, 117, 118, 120, 139, 140, 144, 145, 5, 155, 9, 60, 141, 75, 27, 67, 71, 143, 68, 72, 44, 94, 33, 100, 10, 142, 76, 28, 108, 61, 147, 11, 109, 62, 148, 114, 128, 134, 135, 136, 156, 35, 126, 24, 25, 31, 46, 52, 91, 104, 105, 116, 125, 127, 130, 131, 132, 137, 138, 149, 151, 154, 157, 158, 159, 170, 171 };

	private static final int[] rotationBlocks = new int[]{ 17, 50, 54, 75, 76, 26, 29, 33, 34, 53, 67, 108, 109, 114, 128, 134, 135, 136, 156, 63, 64, 71, 66, 27, 28, 65, 68, 61, 23, 69, 77, 143, 93, 94, 96, 107, 120, 131, 144, 145, 62, 99, 100, 106, 127, 130, 145, 149, 154, 157, 158, 170, 86, 91 };
	
	static {
		Arrays.sort(dataBlocks);
		Arrays.sort(rotationBlocks);
	}

	public static boolean blockHasNoData( int id ) {
		return Arrays.binarySearch(dataBlocks, id) == -1;
	}

	public static boolean blockRequiresRotation( int id ) {
		return Arrays.binarySearch(rotationBlocks, id) != -1;
	}

	public static boolean arrayContainsOverlap( Object[] array1, Object[] array2 ) {
		for ( Object o : array1 ) {

			for ( Object o1 : array2 ) {
				if ( o.equals( o1 ) ) {
					return true;
				}
			}

		}

		return false;
	}

	public static byte rotate( byte data, int typeID, Rotation rotation ) {
		switch ( typeID ) {
			case 17:
			case 170:
				boolean side1 = ( ( data & 0x4 ) == 0x4 );
				boolean side2 = ( ( data & 0x8 ) == 0x8 );

				if ( side1 || side2 ) {
					data = ( byte ) ( data ^ 0xC );
				}
				return data;

			case 50:
			case 75:
			case 76:
				boolean nonDirectional = data == 0x5 || data == 0x6;

				if ( !nonDirectional ) {
					switch ( data ) {
						case 0x1:
							if ( rotation == Rotation.CLOCKWISE ) {
								data = 0x3;
							} else {
								data = 0x4;
							}
							break;

						case 0x2:
							if ( rotation == Rotation.CLOCKWISE ) {
								data = 0x4;
							} else {
								data = 0x3;
							}
							break;

						case 0x3:
							if ( rotation == Rotation.CLOCKWISE ) {
								data = 0x02;
							} else {
								data = 0x1;
							}
							break;

						case 0x4:
							if ( rotation == Rotation.CLOCKWISE ) {
								data = 0x1;
							} else {
								data = 0x2;
							}
							break;
					}
				}

				return data;

			case 26:
			case 127:
			case 149:
				byte direction = ( byte ) ( data & 0x3 );

				byte constant = 1;

				if ( rotation == Rotation.ANTICLOCKWISE ) {
					constant = -1;
				}

				direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 4, 4 ) );
				data = ( byte ) ( ( data & 0xC ) | direction );

				return data;

			case 29:
			case 33:
			case 34:
				direction = ( byte ) ( data & 0x7 );
				System.out.println( direction );

				nonDirectional = direction == 0x0 || direction == 0x1 || direction == 0x6;

				if ( !nonDirectional ) {
					switch ( direction ) {
						case 0x2:
							// North
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x5;
							} else {
								direction = 0x4;
							}
							break;

						case 0x3:
							// South
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x4;
							} else {
								direction = 0x5;
							}
							break;

						case 0x4:
							// West
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x2;
							} else {
								direction = 0x3;
							}
							break;

						case 0x5:
							//East
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x3;
							} else {
								direction = 0x2;
							}
							break;
					}

					data = ( byte ) ( ( data & 0x8 ) | direction );
				}

				return data;

			case 53:
			case 67:
			case 108:
			case 109:
			case 114:
			case 128:
			case 134:
			case 135:
			case 136:
			case 156:

				direction = ( byte ) ( data & 0x3 );


				switch ( direction ) {
					case 0x0:
						// East
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x2;
						} else {
							direction = 0x3;
						}
						break;

					case 0x1:
						// West
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x3;
						} else {
							direction = 0x2;
						}
						break;

					case 0x2:
						// South
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x1;
						} else {
							direction = 0x0;
						}
						break;

					case 0x3:
						// North
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x0;
						} else {
							direction = 0x1;
						}
						break;
				}
				data = ( byte ) ( ( data & 0x4 ) | direction );
				return data;

			case 63:

				constant = 4;

				if ( rotation == Rotation.ANTICLOCKWISE ) {
					constant = -4;
				}

				data = ( byte ) ( MathUtils.positiveMod( ( data + constant ) % 16, 16 ) );

				return data;

			case 64:
			case 71:
				boolean isRealDoor = ( data & 0x8 ) == 0;

				if ( isRealDoor ) {
					direction = ( byte ) ( data & 0x3 );

					constant = 4;

					if ( rotation == Rotation.ANTICLOCKWISE ) {
						constant = -4;
					}

					direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 4, 4 ) );

					data = ( byte ) ( ( data & 0xC ) | direction );
				}

				return data;

			case 66:
				direction = ( byte ) ( data & 0x5 );
				boolean flat = direction == 0x0 || direction == 0x1;

				if ( flat ) {

					constant = 1;

					if ( rotation == Rotation.ANTICLOCKWISE ) {
						constant = -1;
					}

					direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 2, 2 ) );

					data = direction;

				} else {

					if ( data >= 0x6 ) {
						// Is a corner piece
						constant = 1;

						if ( rotation == Rotation.ANTICLOCKWISE ) {
							constant = -1;
						}

						direction = ( byte ) ( MathUtils.positiveMod( ( ( data >> 4 ) + constant ) % 4, 4 ) );

						data = ( byte ) ( direction << 4 );

					} else {
						// Is a rising piece
						switch ( direction ) {
							case 0x2:
								// East
								if ( rotation == Rotation.CLOCKWISE ) {
									direction = 0x4;
								} else {
									direction = 0x5;
								}
								break;

							case 0x3:
								// West
								if ( rotation == Rotation.CLOCKWISE ) {
									direction = 0x5;
								} else {
									direction = 0x4;
								}
								break;

							case 0x4:
								// South
								if ( rotation == Rotation.CLOCKWISE ) {
									direction = 0x3;
								} else {
									direction = 0x2;
								}
								break;

							case 0x5:
								// North
								if ( rotation == Rotation.CLOCKWISE ) {
									direction = 0x2;
								} else {
									direction = 0x3;
								}
								break;
						}

						data = direction;
					}

				}

				return data;


			case 27:
			case 28:
			case 157:
				direction = ( byte ) ( data & 0x5 );
				flat = direction == 0x0 || direction == 0x1;

				if ( flat ) {

					constant = 1;

					if ( rotation == Rotation.ANTICLOCKWISE ) {
						constant = -1;
					}

					direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 2, 2 ) );

					data = direction;

				} else {

					// Is a rising piece
					switch ( direction ) {
						case 0x2:
							// East
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x4;
							} else {
								direction = 0x5;
							}
							break;

						case 0x3:
							// West
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x5;
							} else {
								direction = 0x4;
							}
							break;

						case 0x4:
							// South
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x3;
							} else {
								direction = 0x2;
							}
							break;

						case 0x5:
							// North
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x2;
							} else {
								direction = 0x3;
							}
							break;
					}

					data = direction;


				}

				return data;

			case 65:
			case 68:
			case 61:
			case 62:
			case 23:
			case 130:
			case 154:
			case 158:
				if ( data == 0x0 || data == 0x1 ) {
					return data;
				}

				direction = ( byte ) ( data & 0x7 );
				switch ( direction ) {
					case 0x5:
						// East
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x3;
						} else {
							direction = 0x2;
						}
						break;

					case 0x4:
						// West
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x2;
						} else {
							direction = 0x3;
						}
						break;

					case 0x3:
						// South
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x4;
						} else {
							direction = 0x5;
						}
						break;

					case 0x2:
						// North
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x5;
						} else {
							direction = 0x4;
						}
						break;
				}

				data = direction;
				return data;

			case 69:
				direction = ( byte ) ( data & 0x7 );
				if ( direction >= 0x1 && direction <= 0x4 ) {

					switch ( direction ) {
						case 0x1:
							// East
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x3;
							} else {
								direction = 0x4;
							}
							break;

						case 0x2:
							// West
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x4;
							} else {
								direction = 0x3;
							}
							break;

						case 0x3:
							// South
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x2;
							} else {
								direction = 0x1;
							}
							break;

						case 0x4:
							// North
							if ( rotation == Rotation.CLOCKWISE ) {
								direction = 0x1;
							} else {
								direction = 0x2;
							}
							break;
					}

					data = ( byte ) ( ( data & 0x8 ) | direction );

				} else {
					if ( direction == 0x5 ) {
						data = ( byte ) ( ( data & 0x8 ) | 0x6 );
					} else if ( direction == 0x6 ) {
						data = ( byte ) ( ( data & 0x8 ) | 0x5 );
					} else if ( direction == 0x7 ) {
						data = ( byte ) ( ( data & 0x8 ) );
					} else if ( direction == 0x0 ) {
						data = ( byte ) ( ( data & 0x8 ) | 0x7 );
					}
				}

				return data;

			case 77:
			case 143:
				direction = ( byte ) ( data & 0x7 );
				switch ( direction ) {
					case 0x1:
						// East
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x3;
						} else {
							direction = 0x4;
						}
						break;

					case 0x2:
						// West
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x4;
						} else {
							direction = 0x3;
						}
						break;

					case 0x3:
						// South
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x2;
						} else {
							direction = 0x1;
						}
						break;

					case 0x4:
						// North
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x1;
						} else {
							direction = 0x2;
						}
						break;
				}

				data = ( byte ) ( ( data & 0x8 ) | direction );

				return data;

			case 86:
			case 91:
				direction = ( byte ) ( data & 0x3 );

				if ( data == 0x4 ) {
					data = 0x4;
				} else {
					constant = 1;

					if ( rotation == Rotation.ANTICLOCKWISE ) {
						constant = -1;
					}

					direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 4, 4 ) );

					data = direction;
				}

				return data;

			case 93:
			case 94:
				direction = ( byte ) ( data & 0x3 );

				constant = 1;

				if ( rotation == Rotation.ANTICLOCKWISE ) {
					constant = -1;
				}

				direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 4, 4 ) );

				data = ( byte ) ( ( data & 0xC ) | direction );

				return data;

			case 96:
				direction = ( byte ) ( data & 0x3 );
				switch ( direction ) {
					case 0x2:
						// East
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x0;
						} else {
							direction = 0x1;
						}
						break;

					case 0x3:
						// West
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x1;
						} else {
							direction = 0x0;
						}
						break;

					case 0x0:
						// South
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x3;
						} else {
							direction = 0x2;
						}
						break;

					case 0x1:
						// North
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x2;
						} else {
							direction = 0x3;
						}
						break;
				}

				data = ( byte ) ( ( data & 0xfc) | direction );

				return data;
			case 106:
				if ( data != 0x0 ) {
					if ( rotation == Rotation.CLOCKWISE ) {
						data = ( byte ) ( data << 1 );
					} else {
						data = ( byte ) ( data >> 1 );
					}

					if ( data > 8 ) {
						data = 1;
					} else if ( data == 0x0 ) {
						data = 8;
					}

					return data;
				} else {
					return data;
				}

			case 107:
			case 120:
				direction = ( byte ) ( data & 0x3 );

				constant = 1;

				if ( rotation == Rotation.ANTICLOCKWISE ) {
					constant = -1;
				}

				direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 4, 4 ) );

				data = ( byte ) ( ( data & 0x4 ) | direction );

				return data;

			case 131:
				direction = ( byte ) ( data & 0x3 );

				constant = 1;

				if ( rotation == Rotation.ANTICLOCKWISE ) {
					constant = -1;
				}

				direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 4, 4 ) );

				data = ( byte ) ( ( data & 0xC ) | direction );

				return data;

			case 144:
				direction = data;
				switch ( direction ) {
					case 0x1:
						return data;
					case 0x4:
						// East
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x3;
						} else {
							direction = 0x2;
						}
						break;

					case 0x5:
						// West
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x2;
						} else {
							direction = 0x3;
						}
						break;

					case 0x3:
						// South
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x5;
						} else {
							direction = 0x4;
						}
						break;

					case 0x2:
						// North
						if ( rotation == Rotation.CLOCKWISE ) {
							direction = 0x4;
						} else {
							direction = 0x5;
						}
						break;
				}

				return direction;

			case 145:
				direction = ( byte ) ( data & 0x1 );
				constant = 1;

				if ( rotation == Rotation.ANTICLOCKWISE ) {
					constant = -1;
				}

				direction = ( byte ) ( MathUtils.positiveMod( ( direction + constant ) % 2, 2 ) );

				data = ( byte ) ( ( data & 0xC ) | direction );

				return data;

			case 99:
			case 100:
				direction = data;
				switch(direction) {
					case 0x0:
					case 0x5:
					case 0xA:
					case 0xE:
					case 0xF:
						return data;
					case 0x2:
						//North
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x6;
						} else {
							direction = 0x4;
						}
						break;
					case 0x4:
						//East
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x2;
						} else {
							direction = 0x8;
						}
						break;
					case 0x6:
						//West
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x8;
						} else {
							direction = 0x2;
						}
						break;
					case 0x8:
						//South
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x4;
						} else {
							direction = 0x6;
						}
						break;
					case 0x1:
						//North and West
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x3;
						} else {
							direction = 0x7;
						}
						break;
					case 0x3:
						//North and East
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x9;
						} else {
							direction = 0x1;
						}
						break;
					case 0x7:
						//South and West
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x1;
						} else {
							direction = 0x9;
						}
						break;
					case 0x9:
						//South and East
						if(rotation == Rotation.CLOCKWISE) {
							direction = 0x7;
						} else {
							direction = 0x3;
						}
						break;
				}
				
				return direction;
				
			default:
				return data;
		}


	}
}
