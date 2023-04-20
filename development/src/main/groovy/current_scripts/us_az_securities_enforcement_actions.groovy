import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeAddress
import org.apache.commons.lang.StringUtils


context.setup([socketTimeout: 30000, connectionTimeout: 10000]);


//handleIndexPage("https://www.azcc.gov/securities/enforcement/actions", "Respondent</span></span></span></span></td>");
//handleIndexPage("http://www.azcc.gov/divisions/securities/enforcement/enforce-actions.asp", "Respondent</span></span></span></span></td>");
//handleIndexPage("http://www.azcc.gov/divisions/securities/Enforcementactionsarchive2007-2004.asp", "<table id=\"table67\"");
//handlePdfPage("http://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-1999-2003.pdf?sfvrsn=2e939a51_2");
//handleIndexPage("http://www.azcc.gov/divisions/securities/Enforcementactionsarchive1999-2003.asp", "<table id=\"table67\"");


handleIndexPage("https://www.azcc.gov/securities/enforcement/actions", "Respondent</strong></td>")

def mainUrl = "https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-"
def list = ["2008-2016.pdf?sfvrsn=52ac9829_2","2004-2007.pdf?sfvrsn=a49c3add_2","1999-2003.pdf?sfvrsn=2e939a51_2"] as String[]

def indexUrl
for(int i=0; i<3;i++) {

    indexUrl = mainUrl+list[i]
    handlePdfPage("$indexUrl")
}

def handleIndexPage(indexUrl, startText) {
    indexPage = context.invoke([url: indexUrl, tidy: true, clean: true]);

    context.elementSeek(indexPage, [element: "tr", startText: startText, endText: "</tbody>", greedy: false]).each {tr ->
        tr.stripFormatting();
        entry = context.regexMatch(tr, [regex: "href=\"(.*?)\">(.*?)</a>.*?<td.*?>(.*?)</td>"]);

        if (entry) {
            def url = entry[1];
            def date = entry[2];
            def names = entry[3].stripHtmlTags();

            date =date.toString().replaceAll(/May 28, 202$/, 'May 28, 2021')
            url = sanitizeUrl(url)

            names.replace("&#8217;", "'");
            names.replace(" - Verified Complaint And Application For Injunction Relief", "");
            names.replace(" - This notice was withdrawn.*\$", "");
            names.replace(" et al", "");
            names.replace("Perry and Terry Penny", "Perry Penny ; Terry Penny");
            names.replace("Mr. Vince", "");
            names.replace("Ms. Sharon", "");
            names.replace("&#8220;Skip&#8221;", ""); //nickname
            names.replace("Sharron E. Govan-Smith, Sharon Smith", "Sharron E. Govan-Smith; Sharon Smith");
            names.replace("Jim and Jane Doe Whatcott; and John and Jane Doe Blitz", "Jim Whatcott;Jane Doe Whatcott; John Blitz;Jane Doe Blitz");
            names.replace("Opulent Management Group, Bravura Management Group, Zanadu Construction", "Opulent Management Group;Bravura Management Group;Zanadu Construction");
            names.replace("John S. Mendibles, MGN Enterprises, LTD", "John S. Mendibles; MGN Enterprises, LTD");
            names.replace("Mark E. &amp; Jane Doe Labertew; Les and Jane Doe Fleishmans", "Mark E. Labertew; Jane Doe Labertew; Les Fleishmans; Jane Doe Fleishmans");
            names.replace("Tierra Group, Inc., Preservation Trust Corporation", "Tierra Group, Inc.; Preservation Trust Corporation");
            names.replace("Preservation Trust Company, Partnership Preservation Trust", "Preservation Trust Company;Partnership Preservation Trust");
            names.replace("Limited Partnership, Caterpillar", "Limited Partnership; Caterpillar");
            names.replace("Partnership, Rene", "Partnership; Rene");
            names.replace("Rene L. Couch, Terry Couch", "Rene L. Couch; Terry Couch");
            names.replace("Evelyn Baumgardner, John Doe Baumgardner", "Evelyn Baumgardner; John Doe Baumgardner");
            names.replace("MTE 2013 Trust, Michael Barry Eckerman", "MTE 2013 Trust; Michael Barry Eckerman");
            names.replace(" [aA]nd ", ";");
            names.replace("&amp;", "&")
            names.replace("&amp;APOS;", "'")

            names.replace("\\(CRD\\s*\\#\\s*\\d+\\)", "")
            names.replace(",?\\s*CRD\\s*\\#\\d+\\s*,?","")
            names.replace(",(?= Castle International)",";")
            names.replace(",;", ",");
            names.replace(";(?=Bejahn Zarrinnegar|Ernesto A. Barrueta|Ryan Lee Oliver|American Housing Income Trust)",",")

            names.split(";").each {name ->
                name.trim();
                name.replace("^and ", "");
                name.replace(",\$", "");
                name.replace(",;", ",");
                name.replace(",(?= IARD)", "")
                name.replace("#282751", "")

                def aliasList
                (name,aliasList) =separateAlias(name)



                if (!StringUtils.isBlank(name.trim().toString())) {
                    addEntity(name.trim(),aliasList, url, date);

                }
            }
        }
    }
}

def handleAlias(aliasList, url, date,entityType,entity){
    def list = []

    aliasList.each {
        if (it) {

            it = it.replaceAll(/(?s)\s+/, " ").trim()
            it = it.replaceAll("&AMP;|&", "&amp;").trim()
            it = it.replaceAll("'", "&apos;").trim()
            def aliasType = determineEntityType(it)

            if( aliasType == entityType){
                entity.addAlias(it)
            }else {
                addEntity(it,list,url,date)
            }
        }
    }
}

def addEntity(name,aliasList, url, date) {

    name = sanitizeName(name)
    def nameUpper = name.toString().trim().toUpperCase();
    nameUpper = nameUpper.replaceAll("&AMP;|&", "&amp;")
    nameUpper = nameUpper.replaceAll("'|&amp;APOS;", "&apos;")

    if (nameUpper.startsWith("JOHN DOE") || nameUpper.startsWith("JANE DOE")) {
        return;
    }

    entityType = determineEntityType(nameUpper)

    entity = context.findEntity([name: nameUpper, type: entityType]);

    if (!entity) {
        entity = context.getSession().newEntity();
        entity.setName(nameUpper);
        entity.setType(entityType);
    }
    event = new ScrapeEvent();
    if (date && !StringUtils.isBlank(date.toString())) {
        event.setDate(context.parseDate(date.trim()));

    }
    event.setDescription("Entity appears on the Arizona Corporate Commission Securities Division Enforcement Actions site");
    if (!entity.getEvents().contains(event)) {
        entity.addEvent(event);
    }

    if (!StringUtils.isBlank(url.toString())) {
        if (url.toString().startsWith("/")) {
            url = "http://www.azcc.gov" + url.toString();
        }
        entity.addUrl(url.toString());
    }
    address = new ScrapeAddress();
    address.setProvince("ARIZONA");
    address.setCountry("UNITED STATES");
    entity.addAddress(address);

    handleAlias(aliasList, url, date,entityType,entity)

}

def  determineEntityType(name) {
    def nameUpper = name.toString().toUpperCase();
    def type = context.determineEntityType(nameUpper);


    ["DIVISION", "OF WEALTH", "REMARKETING", "MORTGAGE", "CONNECTION", "INVESTMENTS", "SYSTEMS", "DEVELOPMENT",
     "CONSULTANTS", "ADVISERS", "CAPITAL", "NCGMI", "PURCHASING", "S.A.", "ENTERTAINMENT", "FILMS", "CONSTRUCTION",
     "FLOORING", "CENTER", "PRECISION", "CORPORATIONS", "PARTNERSHIPS", "DESIGN", "WORLDWIDE", "MAJESTY", "TRAVEL",
     "RESORTS", "FINANCIAL", "ESTATE", "INVESTMENT", "PARTNERSHIP", "CO.", "SUPPLY", "ASSISTANCE", "PLANNING","AC","RESIDENCES",
     "USDB","BRIDGES OF ARCADIA ASSISTED LIVING","FLEETRONIX","MYTRADERCOIN.COM","MTCOIN","COININVEST"].each {token ->
        if (nameUpper.contains(token)) {
            type = "O"
        }
    }
    if (nameUpper.equals("ATI") || nameUpper.equals("RGD") || nameUpper.equals("CONCORDE ASSISTED LIVING")||nameUpper.equals("AITI")) {
        type = "O";
    }

    if (nameUpper =~ /(?:DAVID COME VIENS|JAMES L. OPPENHEIMER|ROBERT NEIL STOCK|LISA A. SHACKLEY|ANGELA PACHECO|ANTHONY RAY STACY|DAVID CARROLL LOACH
    |JOHN W. PACHECO|JOSEPH MACK|KEVIN D. GRACE|KIMBERLY BLACKWELL|MARIACRUZ ALVARADO|ROBERT NEIL STOCK|ROSS WEST|THOMAS S. BLACKWELL|BARBARA M. WEST|BEVERLY MICHELE WEST|ZACHARY S. WAGNER
    |STACEY SILBERMAN|JACK COMBS|DAVID R. KOSACK|ALFRED BACA|BUTTERWORTH|BUTTER WORTH|AVISON C.\s*WEST|AC|DAVID WARNER COME|SYDNEY J. OPPENHEIMER)/) {
        type = "P"
    }
    if(nameUpper =~/(?:PARTNERSHIPS|L\.L\.C\.|LTD\.|REFLECTION BAY ASSISTED LIVING|INC\.)/){
        type ="O"
    }

    return type;
}

def handlePdfPage(indexUrl){
    def months = "january|february|march|april|may|june|july|august|september|october|november|december"

    def pdfFile,pdfText
    try {
        pdfFile = context.invokeBinary([url: indexUrl, clean: false])
        pdfText = context.transformPdfToText(pdfFile, null)

    } catch (Exception e) {

    };

    pdfText = sanitizePdf(pdfText,indexUrl)

    def rowMatcher = pdfText =~/(?ism)((?:$months)\s+\d+,\d*)\s+.*?(?=(?:(?:$months)\s+\d+,\d*)|\Z)/
    while (rowMatcher.find()){
        getData(rowMatcher.group(0),indexUrl)
    }
}
def sanitizePdf(data,indexUrl){

    if (indexUrl=="https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-2008-2016.pdf?sfvrsn=52ac9829_2"){
        def months = "january|february|march|april|may|june|july|august|september|october|november|december"
        data = data.toString().replaceAll(/Ocotober/,"October")
            .replaceAll(//,"")
            .replaceAll(/(?ism)(\w+\s+\d+,)\s*([a-z])/,'$1     $2')
            .replaceAll(/(?ism)((?:$months)\s+\d+,)(\s{3,}[\w\s,\.\;\(\#\)\/\-\'\&\"]*?\s+)(\d+)(\s+)(?=\w+)/,'$1$3$2$4')
            .replaceAll(/(?ism)(CRD\s+)(\d+)(\s+#\d+.*?)(\w+\s\d+,)/,'$1$3$4$2')
            .replaceAll(/January 25,3\s+Scott Hutchinson; Jane Doe Hutchinson; Marine\s+2008/,"January 25,2008    Scott Hutchinson; Jane Doe Hutchinson; Marine 3\n")
            .replaceAll(/(              ).*?(WMF Management, L.L.C.; Woodbridge Group of Companies, L.L.C.; Woodbridge Mortgage)/,'October 4,2016  $2')
            .replaceAll(/(                 )(Visionary Business Works, Inc\. d\.b\.a\. Fleetronix; Robert Brian Brauer; Melissa Brauer; Timothy)\s+(June 29, 2016)\s+(John Wales; Stacey Wales)/,'$3   $2 $4')
            .replaceAll(/                LoanGo Corporation; Justin C. Billingsley; Heather Billingsley; Jeffrey Scott Peterson; John Keith\s+June 30, 2015\s+Ayers; Jennifer Ann Brinkman-Ayers/,"\nJune 30, 2015   LoanGo Corporation; Justin C. Billingsley; Heather Billingsley; Jeffrey Scott Peterson; John Keith Ayers; Jennifer Ann Brinkman-Ayers")
            .replaceAll(/                Concordia Financing Company, Ltd. \(a.k.a. Concordia Finance\); ER Financial & Advisory Services,\s+May 7, 2015\s+L.L.C.; Lance Michael Bersch; David John Wanzek; Linda Wanzek/,"May 7, 2015   Concordia Financing Company, Ltd. (a.k.a. Concordia Finance); ER Financial & Advisory Services, L.L.C.; Lance Michael Bersch; David John Wanzek; Linda Wanzek")
            .replaceAll(/(                )(Thomas Laurence Hampton \(CRD #2470192\); Stephanie Yager; Timothy D. Moran \(CRD)/,'December 4,2012  $2')
            .replaceAll(/(                )(Tri-Core Companies, L.L.C.; Tri-Core Mexico Land Development, L.L.C.; Tri-Core Business)/,'\nNovember 8,2012  $2')
            .replaceAll(/(              )(Christopher Dean Dedmon \(CRD #3015575\); Kimberly Dedmon; Robert R. Cottrell \(a\.k\.a\. Rob)/,'August 10,2012  $2')
            .replaceAll(/(              )(Thomas Laurence Hampton \(CRD #2470192\); Stephanie Yager; Timothy D. Moran \(CRD)/,'July 12,2012  $2')
            .replaceAll(/(               )(Crystal Pistol Resources, L.L.C.; Crystal Pistol Management, L.L.C.; Liberty Bell Resources I L.L.C.;)\s+(April 5, 2012)\s+(Peter Pocklington; John M. McNeil)/,'\n$3  $2 $4')
            .replaceAll(/(               )(Benjamin M. Cvetkovich; Sterling Investments Group International, L.L.C.; George A. Pruden;)\s+(March 30, 2012)\s+(Janet F. Pruden)/,'\n$3  $2 $4')
            .replaceAll(/(               )(Seed Corporation; Randall Duane Simonson; Marilyn J. Simonson; Karl Henry Rehberg \(a.k.a.)\s+(March 30, 2012)\s+(Shawn Pierce\); Helen Rehberg \(a.k.a. Lisa Pierce\))/,'\n$3  $2 $4')
            .replaceAll(/(              )(David Paul Smoot; Marie Kathleen Smoot \(a.k.a. Kathy Smoot\); Native American Water, L.L.C.)\s+(October 20,2011)/,'\n$3  $2')
            .replaceAll(/(              )(Terry L. Samuels; Elizabeth Samuels; James F. Curcio; Jill L. Curcio; 3-CG, L.L.C.; Choice Property)\s+(July 18, 2011)/,'\n$3  $2\n              ')
            .replaceAll(/(                )(Craig Randal Munsey; Jane Doe Munsey; Marketing Reliablity Consulting, L.L.C. d.b.a. MRC)\s+(May 23, 2011)/,'\n$3   $2')
            .replaceAll(/(                )(Kent M. Axtell, d.b.a. Sherlock Homes and Finding Homes for Investors; Janis C. Axtell; Executive)\s+(May 2, 2011)/,'\n$3   $2')
            .replaceAll(/(                )(Fred Otto Bohn; Marsha Bohn; Capital Oil & Gas, Limited \(a.k.a. Capital Oil & Gas, Ltd. f.k.a.)\s+(April 6, 2011)/,'\n$3   $2')
            .replaceAll(/(              )(Maglev Wind Turbine Technologies, Inc.; Maglev Renewable Energy Resources, Inc.; Renewable)/,'\nMarch 1, 2011  $2')
            .replaceAll(/( \s+)(David Come Viens \(f.k.a. David Warner Come, David Zachery Combs, and David Zachery\); Scott)/,'December 15,2010 $2')

            .replaceAll(/( \s+)(Kenneth Joseph Plein; Mary Kathryn Plein \(a.k.a. Mary Kay Plein\); Kenneth Joseph Plein and)\s+(December 15,2010)/,'\n$3  $2\n')

            .replaceAll(/( \s+)(Five Star Tree Service and Landscapes, L\.L\.C\. \(a\.k\.a\. Five Star Tree Service\); Richard McCullum,)\s+(August 3,2010)\s+(Jr\.; Leah Atwood)/,'\n$3  $2 $4')
            .replaceAll(/( \s+)(Steven Dale Morrey d.b.a. Mayet, Inc.; Jennifer Morrey; Mayet Holdings, L.L.C. d.b.a. Mayet)\s+(August 2, 2010)\s+(Holdings, Ltd.)/,'\n$3  $2 $4')
            .replaceAll(/(Ethan Sturgis Day; Theresa Day; Silversprings Real Estate Development & Investments, L.L.C.)/,'\nJuly 21, 2010 $1')
            .replaceAll(/( \s+)(Todd Allan Hoss individually and d\.b\.a\. Sellman Weis Mortgage & Investment Corporation and)\s+(Chesterfield Mortgage Investment Corporation; Jane Doe Hoss; Rick Sellman individually and)\s+(April 19, 2010)/,'\n$4  $2 $3')
            .replaceAll(/( \s+)(David E. Walsh and Lorene Walsh d.b.a. New York Networks, Inc. f.k.a. Jubilee Acquisition)\s+(February 19,2010)/,'\n$3  $2\n   ')
            .replaceAll(/( \s+)(Carol Dee Aubrey; John Doe Aubrey; Progressive Energy Partners, L.L.C.; Progressive Energy)\s+(February 4,2010)/,'\n$3 $2\n        ')
            .replaceAll(/( \s+)(Barron Wilson Thomas; Barron Thomas Scottsdale, L\.L\.C\.; Barron Thomas Aviation, Inc.; Barron)\s+(January 5, 2010)/,'\n$3  $2')
            .replaceAll(/( \s+)(Miko D. Wady; Jennifer L. Savage \(f.k.a. Jennifer L. Wady\); Nato Enterprises, L.L.C.; Malika S.)\s+(December 23,2009)/,'\n$3  $2\n      ')
            .replaceAll(/( \s+)(Jolleen K. Hansen; Nathan E. Hansen; Thomas S. Blackwell \(CRD #4370822\); Kimberly Blackwell;)\s+(July 31, 2009)/,'\n$3  $2')
            .replaceAll(/( \s+)(Michael C. Reynolds; Tanzia Reynolds; Cash 2 U, L.L.C.; Dos Ninas, L.L.C.; Par 3 Management,)\s+(July 27, 2009)/,'\n$3  $2')
            .replaceAll(/( \s+)(Green Panel Corporation; Joseph Samuel Burton; Bonnie Eileen Burton; Panelized Building)\s+(May 20, 2009)/,'\n$3  $2')
            .replaceAll(/( \s+)(Lamont C. Paterson II a.k.a. Folo Patterson d.b.a. Olof Enterprises; Eletrea L. Patterson; Olof)\s+(April 22, 2009)/,'\n$3  $2')
            .replaceAll(/( \s+)(Robert W. Mangold; Michelle M. Mangold; One Source Mortgage & Investments, Inc.; Strategic)\s+(April 21, 2009)/,'\n$3  $2')
            .replaceAll(/( \s+)(David W. Cole; Siiri Cole; Highline Estates, L.L.C.; Mutual Financial Services, L.L.C. d.b.a. MFS Real)\s+(February 19,2009)/,'\n$3  $2\n           ')
            .replaceAll(/( \s+)(Stanley Lane Boblett \(CRD #2209980\) a.k.a. Lane Boblett; Antonia Boblett a.k.a. Toni Boblett)\s+(August 1, 2008)/,'\n$3  $2')
            .replaceAll(/( \s+)(Mark W. Bosworth; Lisa A\. Bosworth; Stephen G\. Van Campen; Diane V\. Van Campen; Michael J.)\s+(July 3, 2008)/,'\n$3  $2')
            .replaceAll(/( \s+)(Panama Capital Funding, L.L.C.; Benjamin Ray O'Toole; International Funding Network;)\s+(March 20, 2008)/,'\n$3  $2')
            .replaceAll(/( \s+)(John W. Pacheco; Angela Pacheco; Bill L. Walters; Jacquelyn Walters; Financial American)\s+(June 26, 2009)/,'\n$3  $2')
            .replaceAll(/( \s+)(Radical Bunny, L.L.C.; Horizon Partners, L.L.C.; Tom Hirsch a.k.a. Tomas N. Hirsch; Diane Rose)\s+(March 12, 2009)/,'\n$3  $2')
            .replaceAll(/(?:October 4,2016)(?=\s+Mortgage Investment Fund 3,)/,"          ")
            .replaceAll(/(?:December 4,2012|July 12, 2012)(?=\s+#2326078\))/,"          ")
            .replaceAll(/(?:November 8,2012)(?=\s+Development, L.L.C.; ERC Compactors,)/,"          ")
            .replaceAll(/(?:August 10,2012)(?=\s+Cottrell\); SDC Montana Consulting)/,"           ")
            .replaceAll(/(?<=Jane Doe Mazur;\s)March 1, 2011\s+(?=Ronnie Williams;)/,"              ")
            .replaceAll(/(?<=Assisted Living Residences at\s)December 15,2010\s+(?=51; ALDCO Investment #4 L\.L\.C\.)/,"            ")
            .replaceAll(/(?:July 21, 2010)(?=\s+f.k.a. Silverleaf Real)/,"             ")
            .replaceAll(/\s+(?=L.L.C.)/," ")
            .replaceAll(/(?:\(CRD\s*\#\s*\d+\)|CRD\s*\#\d+,?)/,"")
            .replaceAll(/, Co-Trustees of the Plein Family Trust u.t.a.\s+dated\s+December 1, 1993/,"")
            .replaceAll(/(?<=Todd R. Nuttall; Magdalena Homes, L\.L\.C\.),(?= Rotall Marketing Group L.L.C. d.b.a. Direct Rev)/,";")
            .replaceAll(/William N. Nordstrom, Linda Nordstrom,/,"William N. Nordstrom; Linda Nordstrom;")
            .replaceAll(/;\s*;/,";")
            .replace(/a.k.a., d.b.a., a.b.n./,"a.k.a.")
            .replaceAll(/(?<=Five Star Tree Service )and/,";")
            .replaceAll(/(?<=West Mining )and(?= Innovations, Inc.)/,";")
            .replaceAll(/(?<=Shadow Beverages )and(?= Snacks, L.L.C.)/,";")
            .replaceAll(/(?<=Ride Hard )and(?= Pray, L.L.C.)/,";")
            .replaceAll(/(?<=Kenneth Joseph Plein )and/,";")
            .replaceAll(/(?<=Ulf Olof Holgersson )and(?= Laverne J. Abe)/,";")
            .replaceAll(/(?<=Jere Parkhurst )and(?= Michelle Parkhurst)/,";")
            .replaceAll(/(?<=David E. Walsh )and(?= Lorene Walsh)/,";")
        return data
    }

    if (indexUrl=="https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-2004-2007.pdf?sfvrsn=a49c3add_2"){
        def months = "january|february|march|april|may|june|july|august|september|october|november|december"

        data = data.toString().replaceAll(//,"")
            .replaceAll(/(?ism)(\w+\s+\d+,)\s*([a-z])/,'$1     $2')
            .replaceAll(/(?ism)((?:$months)\s+\d+,)(\s{3,}[\w\s,\.\;\(\#\)\/\-\'\&\"]*?\s+)(\d+)(\s+)(?=\w+)/,'$1$3$2$4')
            .replaceAll(/(November)\s+(Ronald James Strayer; Jane Doe Strayer; Jeffrey Stanford Ryan; Lisa Scott; RJSR Land)\s+(16, 2007)\s+(Developement CR)/,'\n$1 $3  $2 $4\n')
            .replaceAll(/(Veronica Alexander Leigh f\.k\.a\. Candice Anna Gill; Charles William Gill III; Charles Gill and Chuck)\s+(March 28, 2007)\s+(Gill; CAG Financial, LLC; CAG Financial Services, LLC; and Leigh & Associates, LLC)/,'\n$2 $1 $3\n')
            .replaceAll(/(Owen A. Vilan and Lucina Vilan \(a.k.a. Lucy Vilan\); Saguaro Investments, Inc. f.k.a. Vilan)\s+(March 20, 2007)\s+(Enterprises, Inc.)/,'\n$2 $1 $3\n')
            .replaceAll(/(AGRA-Technologies, Inc\. a\.k\.a\. ATI; William Jay Pierson a\.k\.a\. Bill Pierson and Sandra Lee Pierson)\s+(October 18,2006)/,'\n$2   $1\n')
            .replaceAll(/(Edward A. Purvis and Maureen H. Purvis; Gregg L. Wolfe and Allison A. Wolfe; Nakami Chi Group)\s+(October 03,2006)/,'\n$2   $1\n')
            .replaceAll(/( \s+)(Trend Management Group, Inc.; Scott Renny Bogue, Sr. \(CRD# 1588216\) and Arlene Jane Bogue;)/,'September 5,2006   $2')
            .replaceAll(/(August 28,12     The  Percent Fund I, LLC;)\s+(Coyote Growth Management, LLC; Michael Joseph Hannan and Jane)\s+(2006)\s+(Doe Hannan; Sam Ahdoot a\.k\.a\. Sam Ahdout and Jane Doe Ahdoot)/,'August 28,2006     The 12 Percent Fund I, LLC;$2 $4\n')
            .replaceAll(/( \s+)(Lori Lee Spranger a.k.a. Lori Moriarty and\/or Lori Lee Levandowski and\/or Lori Gessell d.b.a.)/,'February 10,2006   $2')
            .replaceAll(/(February 10,90     Vector  Debt Purchasing;)\s+(Martin Otto Spranger a.k.a. Martin Otto Spranger III\); Michael)\s+(2006)/,'        Vector 90 Debt Purchasing;$2')
            .replaceAll(/( \s+)(Michael Giannantonio a\.k\.a\. Michael David Turley d\.b\.a\. Nitefire Entertainment; Karen Lynn)\s+(December 13,2005)/,'\n$3   $2\n')
            .replaceAll(/( \s+)(Bruce R. Goldman, d.b.a. Opulent Management Group, Bravura Management Group, Zanadu)\s+(November 14,2005)/,'\n$3   $2\n')
            .replaceAll(/( \s+)(Brixon Group Ltd\.; Joseph Wayne McCool a\.k\.a\. Joe McCool and Jane Doe McCool; Donald John)\s+(August 9, 2005)/,'\n$3   $2\n')
            .replaceAll(/( \s+)(Thomas C. Messina a.k.a. Thomas Campbell Messina and Tom C. Messina and Donna M.)\s+(July 20,2005)/,'\n$3   $2')
            .replaceAll(/( \s+)(H. Jon Kunowski; Precision Model and Design, Inc.; Air Lase, Inc.; American Innovative Research,)\s+(May 24, 2004)/,'\n$3   $2\n')
            .replaceAll(/( \s+)(Multi Media Technology Ventures, Ltd.; Biltmore Group, Inc.; Global Trek Xploration Corp.; Chris)\s+(May 17, 2004)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(Fountain Capital Management, LLC c\/o David A. Fazio; Integrowth Financial Group c\/o Roger)\s+(May 7, 2004)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(H. Jon Kunowski; Precision Model and Design, Inc.; Air Lase, Inc.; American Innovative Research,)\s+(March 24, 2004)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(Multimedia Technology Ventures, LTD. a.k.a. MMTV, Ltd.; Biltmore Group, Inc.; Global Trek)\s+(March 4, 2004)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(M.A.C Investments, Inc.; M.A.C Investments Sales, Inc.; M.A.C Investment Sales, Inc.; Maricruz)\s+(February 15,2005)/,'\n$3    $2\n')
            .replaceAll(/(?:September 5,2006)(?=\s+Capital, LLC; Linda Bryant Jordan a.k.a.)/,"          ")
        return data
    }

    if (indexUrl=="https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-1999-2003.pdf?sfvrsn=2e939a51_2"){

        def months = "january|february|march|april|may|june|july|august|september|october|november|december"

        data = data.toString().replaceAll(//,"")
            .replaceAll(/September13,/,'September 13,')
            .replaceAll(/December 11\./,'December 11,')
            .replaceAll(/August, 10/,'August 10,')
            .replaceAll(/(?ism)(\w+\s+\d+,)\s*([a-z])/,'$1     $2')
            .replaceAll(/(?ism)((?:$months)\s+\d+,)(\s{3,}[\w\s,\.\;\(\#\)\/\-\'\&\"]*?\s+)(\d+)(\s+)(?=\w+)/,'$1$3$2$4')
            .replaceAll(/( \s+)(International Global Positioning, Inc.; John J. Madsen; Michael J. Coker; James w. Dreos d.b.a.)\s+(November 18,2003)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(Yucatan Resorts, Inc.; Yucatan Resorts, S.A.; Resort Holdings International, Inc.; Resort Holdings)\s+(September 18,2003)/,'\n$3     $2\n')
            .replaceAll(/( \s+)(Yucatan Resorts, Inc\., d\.b\.a\. Yucatan Resorts; S\.A\., Resort Holdings International, Inc\., d\.b\.a)\s+(May 20, 2003)/,'\n$3     $2\n')
            .replaceAll(/( \s+)(Ocean International Marketing, LTD.; Heros Global Marketing, LTD.; Seed International, LTD.;)\s+(April 2, 2003)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(Wesley Karban Wyatt and Jane Doe Wyatt d.b.a. The Financial Greenhouse c\/o Harry N. Stone,)\s+(March 17, 2003)/,'\n$3     $2\n')
            .replaceAll(/( \s+)(Tierra Group, a.k.a. Tierra Group Properties, a.k.a. Tierra Group Companies, a.k.a. Tierra Group,)/,'\nJanuary 23,2003   $2')
            .replaceAll(/( \s+)(Scottsdale Financial Funding Group, LLC; Martin & Griffin ,LLC; Gregory B. Gill a.k.a. Gregory P.)\s+(November 14,2002)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(Scottsdale Financial Funding Group, LLC; Martin & Griffin, LLC; Gregory B. Gill a.k.a. Gregory P.)\s+(September 13,2002)/,'\n$3    $2\n')
            .replaceAll(/( \s+)(American National Mortgage; Secura Innovative Investment; Secura Mortgage Management;)\s+(September 5,2002)/,'\n$3      $2\n')
            .replaceAll(/( \s+)(Bob's Cash Express, Inc.; Bob's Land One, Inc.; Challenge\/Land USA, Inc.; Arizona Digital Security)\s+(May 08, 2002)/,'\n$3      $2\n')
            .replaceAll(/( \s+)(Scottsdale Financial Funding Group, LLC; Martin & Griffin, LLC; Gregory B. Gill a.k.a. Gregory P.)\s+(March 29, 2002)/,'\n$3     $2\n')
            .replaceAll(/( \s+)(Netgo, Inc.; Sdic Partnership; M-Corp International; M-Corp International, Ltd. A Turks And)\s+(February 26,2002)/,'\n$3     $2\n')
            .replaceAll(/( \s+)(Creative Financial Funding, LLC; American Money Power, Inc.; Federal Capital, LLC; Corporate)\s+(October 05,2001)/,'\n$3     $2\n')
            .replaceAll(/( \s+)(Hotel Connect LLC's #100-1100 ; Mark Alan Melkowski, SR.; Eagle Communications, Inc.; Ronald)/,'\nJuly 18, 2001   $2\n')
            .replaceAll(/( \s+)(Easy Money Auto Leasing, Inc.; Superior Financial Services, Inc.; James Anthony Cicerelli; David)\s+(May 14, 2001)/,'\n$3   $2\n')
            .replaceAll(/( \s+)(Accelerated Success Inc.; Kenneth R. Morris; Robert D. Pierson; Integrity Assured Life)\s+(April 10, 2001)/,'\n$3   $2')
            .replaceAll(/( \s+)(Mobile Cash Systems LLC; World Wireless Solutions Inc.; World Electronic Payment Solutions)\s+(January 25,2001)/,'\n$3   $2\n')
            .replaceAll(/( \s+)(Charles Ray Stedman; Wendell T. Decker, JR.; Oxford Development, LLC; Profutura, LLC; CNT)\s+(December 28,2000)/,'\n$3   $2\n')
            .replaceAll(/December 27,     Tower Equities, Inc; Philip A. Lehman/,'December 27,2000     Tower Equities, Inc; Philip A. Lehman')
            .replaceAll(/(September 27,)(2001)\s+(Arthur Andersen L.L.P. - This notice was withdrawn on January 23, 2001. See January 19,)\s+(2000)/,'\n$1$4     $3$2\n')
            .replaceAll(/( \s+)(Joseph Michael Guess, SR.; Progressive Financial Management; James Douglas Sherriffs; Richard)\s+(April 06, 2000)/,'\n$3    $2\n')
            .replaceAll(/- This notice was withdrawn on January 23, 2001\. See January 19,2001\s+Civil Complaint\./,"")
            .replaceAll(/(?:January 23,2003)(?=\s+Company, Partnership Preservation Trust,)/,"          ")
            .replaceAll(/(?<=Cornerstone Senior Planning;)(?:\s+July 18, 2001)/,"          ")
            .replaceAll(/(?<=Inc.; Keith B.)(?:\s+2000)/,"          ")
            .replaceAll(/(?<=Harry N. Stone),\s+Attorney at Law/,"")
            .replaceAll(/and (?=Jane Doe Wyatt)/,";")
            .replaceAll(/(?<=Camelback, Ltd. A Turks)\s+And(?= Caicos Corporation)/,";")
            .replaceAll(/(?<=M-Corp International, Ltd. A Turks )And/,";")
        return data
    }
}

def getData(data,url){

    if (url==("https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-2008-2016.pdf?sfvrsn=52ac9829_2")){
        def dateMatcher = data =~/(\w+\s+\d+,\s*\d+)\s+/
        def date
        if(dateMatcher.find()){
            date=dateMatcher.group(1)
            data = data.toString().replaceAll(/(\w+\s+\d+,\s*\d+)\s+/,"")

        }
        def names = data

        names.replace("\\(.*?\\)", "");
        names.replace("\\(", "");
        names.replace("\\)", "");
        names.replace(" a.?k.?a.? ", ";");
        names.replace(" d.?b.?a.? ", ";");
        names.replace(" f.?k.?a.? ", ";");
        names.replace(" c/o ", ";");
        names.replace("and/or", ";");
        names.replace(", currently known as", ";");
        names.replace("&#8217;", "'");
        names.replace(" - Verified Complaint And Application For Injunction Relief", "");
        names.replace(" - This notice was withdrawn.*\$", "");
        names.replace(" et al", "");
        names.replace("Perry and Terry Penny", "Perry Penny ; Terry Penny");
        names.replace("Mr. Vince", "");
        names.replace("Ms. Sharon", "");
        names.replace("&#8220;Skip&#8221;", ""); //nickname
        names.replace("Sharron E. Govan-Smith, Sharon Smith", "Sharron E. Govan-Smith; Sharon Smith");
        names.replace("Jim and Jane Doe Whatcott; and John and Jane Doe Blitz", "Jim Whatcott;Jane Doe Whatcott; John Blitz;Jane Doe Blitz");
        names.replace("Opulent Management Group, Bravura Management Group, Zanadu Construction", "Opulent Management Group;Bravura Management Group;Zanadu Construction");
        names.replace("John S. Mendibles, MGN Enterprises, LTD", "John S. Mendibles; MGN Enterprises, LTD");
        names.replace("Mark E. &amp; Jane Doe Labertew; Les and Jane Doe Fleishmans", "Mark E. Labertew; Jane Doe Labertew; Les Fleishmans; Jane Doe Fleishmans");
        names.replace("Tierra Group, Inc., Preservation Trust Corporation", "Tierra Group, Inc.; Preservation Trust Corporation");
        names.replace("Preservation Trust Company, Partnership Preservation Trust", "Preservation Trust Company;Partnership Preservation Trust");
        names.replace("Limited Partnership, Caterpillar", "Limited Partnership; Caterpillar");
        names.replace("Partnership, Rene", "Partnership; Rene");
        names.replace("Rene L. Couch, Terry Couch", "Rene L. Couch; Terry Couch");
        names.replace("Evelyn Baumgardner, John Doe Baumgardner", "Evelyn Baumgardner; John Doe Baumgardner");
        names.replace("David Nutter / North American", "David Nutter ; North American");
        names.replace(", Attorney at Law", "");
        names.replace(" [aA]nd ", ";");
        names.replace("&amp;", "-AMP-");
        names.trim()

        names.toString().replaceAll(/\s+/," ").split(/[;]/).each { name ->

            name = name.trim();
            name.replace("-AMP-", "&amp;");
            name.replace("^and ", "");
            name.replace(",\$", "");

            def aliasList
            (name, aliasList) = separateAlias(name)

            name = sanitizeName(name)
            date = date.replaceAll(/(\d{4})$/,' $1')
            if (!StringUtils.isBlank(name.trim().toString())) {
                addEntity(name,aliasList, url, date);
            }
        }
    }
    if (url=="https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-2004-2007.pdf?sfvrsn=a49c3add_2"){

        def dateMatcher = data =~/(\w+\s+\d+,\s*\d+)\s+/

        def date
        if(dateMatcher.find()){
            date=dateMatcher.group(1)
            data = data.toString().replaceAll(/(\w+\s+\d+,\s*\d+)\s+/,"")
        }
        def names = data.toString().replaceAll(/,\s*(?=Ltd\.?|LTD\.?|L\.L\.C\.|LLC|Inc\.?|LLP)/," ")
        names = names.replaceAll(/(?<=Chris Corbett |Graham Inch |C. Ronald Paxon |John E. Shannon |Kevin Polardi |Maria Elena Gonzalez|Cameron Guy Campbell )and/,";")
        names = names.replaceAll(/(?<=Gary M. Milley |Scot Alan Oglesby |Felix L. Daniel Sr\. |Charles Gill |Ryan James Herndon |Edward A. Purvis )(?:And|and)/,";")
        names = names.replaceAll(/(?<=Maria Elena Gonzalez )(?:and)/,";")
        names = names.replaceAll(/and(?= Jane Doe Paxon| Sydney J. Oppenheimer| Barbara Cullison| Jane Doe Sheehy| Jane Doe Moriarty| Jane Doe Hannan)/,";")
        names = names.replaceAll(/and(?= Jane Doe Marx| Jennifer Keaton| Gail A. Groh| Mary Elizabeth Ohst| Christine M. Tencza| Linda Jean Fraleigh| Jane Doe Corbett)/,";")
        names = names.replaceAll(/and(?= John Doe Gonzalez| Margaret Rhodes| Kimberly Dedmon| Jane Doe Nutterfe| Polly P. McMillan| Allison A. Wolfe)/,";")
        names = names.replaceAll(/and(?= Jane Doe Voight| Jane Doe Smart)/,";")
        names = names.replaceAll(/and(?= Jane Doe Hodges| Daniel B. Waters)/,";")
        names = names.replaceAll(/(?<=John S. Mendibles),/,";")
        names = names.replaceAll(/(?<=Mill Direct Flooring LLC|Opulent Properties LLC);/," ")
        names = names.replaceAll(/Jim and Jane Doe Whatcott/,"Jim Whatcott ; Jane Doe Whatcott")
        names = names.replaceAll(/John and Jane Doe Blitz/,"John Blitz ; Jane Doe Blitz")
        names = names.replaceAll(/(?<=Kevin H. )and(?= Jane Doe Krause)/,";")
        names = names.replaceAll(/(?<=Precision Model )and(?= Design Inc.)/,";")
        names = names.replaceAll(/(?<=David A. )and(?= Deborah Fazio)/,";")
        names = names.replaceAll(/(?<=Donald )and(?= Helen Abernathy)/,";")
        names = names.replaceAll(/(?<=Stephen A. )and(?= Jane Doe Hiltbrand)/,";")
        names = names.replaceAll(/; llen and Jane Doe Stout, Sr.; Allen and Jane Doe Stout, Jr./, "; Allen Stout Sr.; Jane Doe Stout; Allen Stout Jr.; Jane Doe Stout");
        names = names.replaceAll(/Owen A. Vilan and Lucina Vilan/,"Owen A. Vilan ; Lucina Vilan")
        names = names.replaceAll(/(?<=Scott Renny Bogue, Sr. \(CRD# 1588216\))\s+and/,";")
        names = names.replaceAll(/Mark E. & Jane Doe Labertew/,"Mark E. Labertew ; Jane Doe Labertew")
        names = names.replaceAll(/Les and Jane Doe Fleishmans/,"Les Fleishmans ; Jane Doe Fleishmans")
        names = names.replaceAll(/Perry and Terry Penny/,"Perry Penny ; Terry Penny")

        names.toString().replaceAll(/\s+/," ").split(/[;]/).each { name ->

            name = name.trim()
            name = name.replaceAll(/\(CRD\s*\#\s*\d+\)/,"")
            name = name.replaceAll(/,(?=\s*(?:a\.k\.a\.|S\.A\.|SR\.|JR\.|Sr\.|d\.b\.a\.?|Jr\.|and\/or))/,"")

            def aliasList
            (name, aliasList) = separateAlias(name)
            name = sanitizeName(name)

            date = date.replaceAll(/(\d{4})$/,' $1')

            if (!StringUtils.isBlank(name.trim().toString())) {
                addEntity(name,aliasList, url, date);
            }
        }
    }
    if (url=="https://www.azcc.gov/docs/default-source/securities-files/actions/enforcement-actions-1999-2003.pdf?sfvrsn=2e939a51_2"){

        def dateMatcher = data =~/(\w+\s+\d+,\s*\d+)\s+/

        def date
        if(dateMatcher.find()){
            date=dateMatcher.group(1)
            data = data.toString().replaceAll(/(\w+\s+\d+,\s*\d+)\s+/,"")
        }
        def names = data.toString().replaceAll(/,\s*(?=Ltd\.?|LTD\.?|L\.L\.C\.|LLC|Inc\.?|LLP)/," ")
        names = names.replaceAll(/(?<=Edmond L. Lonergan |Steven C. Bond |George Ioannou |Mark E. Labertew |Les Fleishman |Ralph Shaul)(?:and)/,";")
        names = names.replaceAll(/(?<=Clyde F. Wagnon |Jeri Woods |Douglas Warren |Robert D. Bjerken |John R. Wallrich |Phil Vigarino |Mark Kesler )(?:and)/,";")
        names = names.replaceAll(/(?<=National Advisory Services |Guildmark Industries |Ralph Shaul |Larry William Dunning )(?:and)/,";")
        names = names.replaceAll(/and(?= Lory Kelly| Lori Kelly| Jane Doe Brown| Karen Sanchez| Robin Frost| Jane Doe Ferreira)/,";")
        names = names.replaceAll(/and(?= Joan Doe Warren| Jane Doe Dreos| Gail Caspare)/,";")
        names = names.replaceAll(/,(?= Healthcare Purchasing Alliance Inc.| Preservation Trust Corporation| Partnership Preservation Trust| Caterpillar Foundation Properties| Rene L. Couch| Terry Couch)/,';')
        names = names.replaceAll(/,(?= John Doe Baumgardner)/,';')
        names = names.replaceAll(/\/(?=Land USA Inc.| North American Insurance Services LLC)/,';')

        names.toString().replaceAll(/\s+/," ").split(/[;]/).each { name ->

            name = name.trim()
            name = name.replaceAll(/\(CRD\s*\#\s*\d+\)/,"")
            name = name.replaceAll(/(?<=Alvin Charles Johnson),/,"")
            name = name.replaceAll(/,(?=\s*(?:a\.k\.a\.|S\.A\.|SR\.|JR\.|currently known as|d\.b\.a\.?|Jr\.?))/,"")
            name = name.replaceAll(/currently known as/,"d.b.a.")

            def aliasList
            (name, aliasList) = separateAlias(name)

            name = sanitizeName(name)

            date = date.replaceAll(/(\d{4})$/,' $1')

            if (!StringUtils.isBlank(name.trim().toString())) {
                addEntity(name,aliasList, url, date);
            }
        }
    }
}
def separateAlias(name){

    name = name.toString().replaceAll(/,\s*(?=Ltd\.|L\.L\.C\.|Inc\.|INC\.)/," ")

    def aliasRegex = /(?i)(?:\(.*\)|\(?\s*(?:a\.k.a\.|f\.k\.a\.|d\.b\.a\.?|c\/o).*)/
    def aliasMatcher = name =~ aliasRegex
    def alias
    def aliasList = []

    while (aliasMatcher.find()) {

        def aliasIdentifierRegex = /(?i)(?:\(?\s*(?:f\.k\.a\.|d\.b\.a\.?|a\.k\.a\.)|\sand\/or|\sand as\s|, and|\sand\s|,|c\/o)/
        alias = aliasMatcher.group(0)
        name = name.toString().replaceAll(aliasRegex, "").trim()

        if (alias =~ aliasIdentifierRegex) {
            aliasList1 = alias.toString().split(aliasIdentifierRegex).collect({ it -> return sanitizeAlias(it.trim()) })
            aliasList1.each { it ->
                aliasList.add(it)
            }
        }
        else {
            aliasList.add(sanitizeAlias(alias))
        }
    }
    return [name, aliasList]
}
def sanitizeName(name){
    name = name.toString()
    name = name.replaceAll(/as Trustee of MLC Living Trust Dated 3-17-1999/,"")
    name = name.replaceAll(/(?:individually and)/,"")
    name = name.replaceAll(/- Verified Complaint And Application For Injunction Relief/,"")
    name = name.replaceAll(/(?:,|, )$/,"")
    name = name.replaceAll(/^\s*and /,"")
    name = name.replaceAll(/(?:\#100-1100|, Attorney at Law)/,"")
    name = name.replaceAll(/^(?:\'|\")/,"")
    name = name.replaceAll(/(?:\'|\")$/,"")
    name = name.replaceAll(/(?<=Premiere Financial Group) et al/,"")
    name = name.replaceAll(/Keith B. "Skip" Davis/,"KEITH B. DAVIS")
    name = name.replaceAll(/(?<=\w) (?=Ltd\.|L\.L\.C\.|Inc\.|INC\.|LLC|LTD\.|LLP)/,", ")
    name = name.replaceAll(/(?<=\w) (?=JR\.?|Jr.?|SR.?|Sr.?)/,", ")
    name = name.replaceAll(/(?<=#\d|10), (?=L\.L\.C\.)/," ")
    name = name.replaceAll(/(?<=Allen Stout|Robert Mattson|Felix L. Daniel|Hartgraves|Verdugo|Hockensmith),/,"")
    name = name.replaceAll(/(?<=A.L. Russell & Associates|Arthur Andersen|Overseas Trading|Marketing Group|Cash Systems|Opulent Properties),/,"")
    name = name.replaceAll(/(?<=Diamond Management|Capital Holdings|Westcap Energy|Weldon|Wireless Solutions|Success|Aeromax|AIO Financial|Brixon Group),/,"")
    name = name.replaceAll(/(?<=University|Lanesborough Financial Group|RE-STAR Holdings|RE-STAR|USDB Group|Verdugo Enterprise|Wealthcorp),/,"")
    name = name.replaceAll(/Mag T, Inc./,"Mag T Inc.")
    name = name.replaceAll(/(?<=Resort Holding[s]? International) (?=S.A.)/,", ")

    return name
}
def sanitizeAlias(alias){

    alias = alias.toString().replaceAll(/\(|\)/,"")
        .replaceAll(/^\./,"")
        .replaceAll(/^(?:\'|\")/,"")
        .replaceAll(/(?:\'|\")$/,"")

    return alias
}
def sanitizeUrl(url){
    url = url.toString().replaceAll(/(?:" data-sf-ec-immutable="|" target="home\.asp)/,"")
        .replaceAll(/^%20/,"")

    return url
}



