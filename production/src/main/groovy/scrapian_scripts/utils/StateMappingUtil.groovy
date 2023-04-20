package scrapian_scripts.utils;

public class StateMappingUtil {
  private stateMap = [:];

  public StateMappingUtil(){
    stateMap['AL'] = 'ALABAMA'
    stateMap['AK'] = 'ALASKA'
    stateMap['AZ'] = 'ARIZONA'
    stateMap['AR'] = 'ARKANSAS'
    stateMap['CA'] = 'CALIFORNIA'
    stateMap['CO'] = 'COLORADO'
    stateMap['CT'] = 'CONNECTICUT'
    stateMap['DE'] = 'DELAWARE'
    stateMap['FL'] = 'FLORIDA'
    stateMap['GA'] = 'GEORGIA'
    stateMap['HI'] = 'HAWAII'
    stateMap['ID'] = 'IDAHO'
    stateMap['IL'] = 'ILLINOIS'
    stateMap['IN'] = 'INDIANA'
    stateMap['IA'] = 'IOWA'
    stateMap['KS'] = 'KANSAS'
    stateMap['KY'] = 'KENTUCKY'
    stateMap['LA'] = 'LOUISIANA'
    stateMap['ME'] = 'MAINE'
    stateMap['MD'] = 'MARYLAND'
    stateMap['MA'] = 'MASSACHUSETTS'
    stateMap['MI'] = 'MICHIGAN'
    stateMap['MN'] = 'MINNESOTA'
    stateMap['MS'] = 'MISSISSIPPI'
    stateMap['MO'] = 'MISSOURI'
    stateMap['MT'] = 'MONTANA'
    stateMap['NE'] = 'NEBRASKA'
    stateMap['NV'] = 'NEVADA'
    stateMap['NH'] = 'NEW HAMPSHIRE'
    stateMap['NJ'] = 'NEW JERSEY'
    stateMap['NM'] = 'NEW MEXICO'
    stateMap['NY'] = 'NEW YORK'
    stateMap['NC'] = 'NORTH CAROLINA'
    stateMap['ND'] = 'NORTH DAKOTA'
    stateMap['OH'] = 'OHIO'
    stateMap['OK'] = 'OKLAHOMA'
    stateMap['OR'] = 'OREGON'
    stateMap['PA'] = 'PENNSYLVANIA'
    stateMap['PR'] = 'PUERTO RICO'
    stateMap['RI'] = 'RHODE ISLAND'
    stateMap['SC'] = 'SOUTH CAROLINA'
    stateMap['SD'] = 'SOUTH DAKOTA'
    stateMap['TN'] = 'TENNESSEE'
    stateMap['TX'] = 'TEXAS'
    stateMap['UT'] = 'UTAH'
    stateMap['VT'] = 'VERMONT'
    stateMap['VA'] = 'VIRGINIA'
    stateMap['WA'] = 'WASHINGTON'
    stateMap['WV'] = 'WEST VIRGINIA'
    stateMap['WI'] = 'WISCONSIN'
    stateMap['WY'] = 'WYOMING'
  }

  def normalizeState(stateIn){
    String spelledOut = stateMap[stateIn];
    return spelledOut ? spelledOut : stateIn;
  }
}