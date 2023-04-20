package scrapian_scripts.utils

class AddressMappingUtil {
	private countryMap = [:];
	private	zipMap = [:];
	private stateMap = [:]
	private zeros = '00000'
	
	def zeroPad(input){
		int counter = 5 - input.length();
		return zeros.substring(0,counter) + input;
	}
	
	public AddressMappingUtil(){	
		String type = '';
		String country = '';
		new File('../assets/config/addressMap.txt').getText('Cp1252').eachLine{
			line->
			if(line.startsWith("**")){
				type = line.substring(2);
				if (type.contains("StateMap")){
					country = type.substring(0,2);
				}
			} else {
				String[] splitUp = line.split('\t');
				if(type == 'ZipMap'){
					ArrayList list = zipMap[splitUp[0]]
					if(!list){
						list = [];
						zipMap[splitUp[0]] = list;
					}
					list.add(zeroPad(splitUp[1]));
				} else if (type == 'CountryMap'){
					countryMap[splitUp[0]] = splitUp[1];
				} else if (type.contains("StateMap")){
					HashMap hashMap = stateMap[country]
					if(!hashMap){
						hashMap = [:];
						stateMap[country] = hashMap;
					}
					hashMap[splitUp[0]] = splitUp[1];
				}
			}
		}		
	}
	
	def mapCountry(countryIn){
		String mapped = countryMap[countryIn];
		return mapped ? mapped : countryIn;
	}
	
	def mapZip(zipIn){
		String retVal = null;
		zipMap.each{key, value ->
			if(value.contains(zipIn)){
				retVal = key;
			}
		}
		return retVal;
	}
	
	def normalizeState(stateIn, countryCode = 'US'){
		HashMap map = stateMap[countryCode];
		String spelledOut = map[stateIn];
		return spelledOut ? spelledOut : stateIn;
	}
	
	def addState(stateIn, stateCodeIn, countryCode){
		HashMap hashMap = stateMap[countryCode]
		if(!hashMap){
			hashMap = [:];
			stateMap[countryCode] = hashMap;
		}
		hashMap[stateCodeIn] = stateIn;
	}
	
	def removeState(stateCodeIn, countryCode){	
		HashMap hashMap = stateMap[countryCode]
		hashMap.remove(stateCodeIn);
	}
	
	def addZip(stateCodeIn, zipCode){
		ArrayList list = zipMap[stateCodeIn]
		if(!list){
			list = [];
			zipMap[stateCodeIn] = list;
		}
		list.add(zeroPad(zipCode));
	}
	
	def removeZip(stateCodeIn, zipCode){
		ArrayList list = zipMap[stateCodeIn]
		if(list){
			list.remove(zeroPad(zipCode));
		}
	}
	
	def addCountry(countryIn, countryCode){		
		countryMap[countryCode] = countryIn;
	}
	
	def removeCountry(countryCode){
		countryMap.remove(countryCode);
	}
	
}
