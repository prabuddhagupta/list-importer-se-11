package current_scripts



def str = "1 37th ACMM M/s. Indo French Bio 1.M/s Indo French Bio Tech Ltd   16          891/1998"

def regExMatcher = str =~ /(?i)([\S && [^0-9]])/
def character

while(regExMatcher.find()){

     character = regExMatcher.group(1)

    println character
}
