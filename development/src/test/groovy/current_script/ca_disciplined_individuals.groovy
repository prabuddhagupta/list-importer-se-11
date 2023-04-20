package current_script

import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils

import java.text.SimpleDateFormat

context.setup([socketTimeout: 50000, connectionTimeout: 50000, retryCount: 5]);
context.getSession().setEscape(true);
context.session.encoding = "UTF-8";
links = new HashSet();
today = new Date();
sdf = new SimpleDateFormat("MM/dd/yyyy");

handleIndexPage(context.invoke([url: "http://www.securities-administrators.ca/rss.aspx", tidy: false]));
//handleDetailPage("http://www.securities-administrators.ca/disciplinedpersons.aspx?id=74&amp;p=2921");

def handleIndexPage(indexPage) {
  context.elementSeek(indexPage, [element: "item"]).each {item ->
    item.stripFormatting();

    link = context.regexMatch(item, [regex: "<link>(.*?)</link>"])[1];
    links.add(link);
  }
  links.each {url -> handleDetailPage(url)}
}

def handleDetailPage(url) {
  def html = context.invoke([url: url.toString().replaceAll(/&amp;/, '&'), tidy: false])
  //println html.toString();
  int i = 0;
  def found = true;
  def juris = '';
  def jurisBody = '';
  def entityName = context.regexMatch(html, [regex: "<span id=\"ctl00_bodyContent_lbl_name\" [^>]*>(.*?)</span>"])[1];
  def jurisArray = context.regexMatch(html, [regex: "<span id=\"ctl00_bodyContent_DataList1_ctl00_lbl_juridiction\" [^>]*><div [^>]*>(.*?)<span [^>]*>(.*?)</span></div></span>"]);
  if (jurisArray) {
    juris = jurisArray[1].replace("\\s*-\\s*", '');
    jurisBody = jurisArray[2];
  }
  if (entityName =~ /(?i)0863220 B.C. Ltd./) {
    println(entityName)
  }
  def sanctions = []
  def orderDates = []
  def bannedUntilDates = []
  def payments = []
  def violations = []
  def docs = []
  while (found) {
    def sanction = context.regexMatch(html, [regex: "<span id=\"ctl00_bodyContent_DataList1_ctl0" + i + "_lbl_sanction\"><div [^>]*>(.*?)</span>"]);
    def orderDate = context.regexMatch(html, [regex: "(?s)<span id=\"ctl00_bodyContent_DataList1_ctl0" + i + "_lbl_listdate\">Date of Order or Settlement</span>\\s*</th>\\s*<td>\\s*(.*?)\\s*</td>"]);
    def bannedUntilDate = context.regexMatch(html, [regex: "(?s)<span id=\"ctl00_bodyContent_DataList1_ctl0" + i + "_list_listbanuntil\">Banned Until</span>\\s*</th>\\s*<td>\\s*(.*?)\\s*</td>"]);
    def payment = context.regexMatch(html, [regex: "(?s)<span id=\"ctl00_bodyContent_DataList1_ctl0" + i + "_lbl_listpayment\">Payment Agreed/Ordered</span>\\s*</th>\\s*<td>\\s*(.*?)\\s*</td>"]);
    def violation = context.regexMatch(html, [regex: "<span id=\"ctl00_bodyContent_DataList1_ctl0" + i + "_lbl_violation\">(.*?)</span>"]);
    def doc = context.regexMatch(html, [regex: "<span id=\"ctl00_bodyContent_DataList1_ctl0" + i + "_lbl_docs\"><div [^>]*>(.*?)</span>"]);
    if (sanction) {
      sanctions.add(sanction ? sanction[1] : null);
      orderDates.add(orderDate ? orderDate[1] : null)
      bannedUntilDates.add(bannedUntilDate ? bannedUntilDate[1] : null)
      payments.add(payment ? payment[1] : null)
      violations.add(violation ? violation[1] : null)
      docs.add(doc ? doc[1] : null)
    } else {
      found = false
    }
    i++;
  }
  def link = url;
  //context.info("Name: " + entityName);
  entityName.replace(", 1st Floor", "") //dont want to track the comma on this one...
  def hasComma = entityName.toString().contains(",");
  // separate aliases
  def aka = "(?:a(?:\\.|)k(?:\\.|)a(?:\\.|)|also known as)"
  entityName.replace("&amp;", "&")
  entityName.replace("&apos;", "'")
  entityName.replace(" #\\d", "")
  entityName.replace("Mortgage and Investment", "Mortgage _SAND_ Investment")
  entityName.replace("Huron and Suncoast", "Huron _SAND_ Suncoast")
  entityName.replace("Maples and White", "Maples _SAND_ White")
  entityName.replace("Sabourin and Sun", "Sabourin _SAND_ Sun")
  entityName.replace("McAlpine/Mansfield, Lynn", "Lynn McAlpine -AKA- Lynn Mansfield")
  entityName.replace("Chelmsford/Dunnville", "Chelmsford_SLASH_Dunnville")
  entityName.replace("Lear, Daniel Kingsley \\(aka Lear, Sir Daniel Kingsley and Swim, Ralph Merle", "Daniel Kingsley Lear-AKA- Sir Daniel Kingsley Lear -AKA- Ralph Merle Swim")
  entityName.replace(" aka ", "-AKA-");
  entityName.replace("aka: ", "-AKA-");
  entityName.replace("(.*?), (.*?) \\($aka ([^,]+?), ([^,]+?), ([^,]+?), and (.*?)\\)", "\$2 \$1 -AKA-\$3 -AKA- \$4 -AKA- \$5 -AKA- \$6 ");
  entityName.replace("(.*?), (.*?) \\($aka ([^,]+?), ([^,]+?) and (.*?)\\)", "\$2 \$1 -AKA-\$3 -AKA- \$4 -AKA- \$5 ");
  entityName.replace("(.*?) \\($aka (.*?)\\), (.*?) \\($aka (.*?)\\)", "\$3 \$1 -AKA-\$4 \$2");
  entityName.replace("(.*?), (.*?) \\($aka (.*?)\\)", "\$2 \$1 -AKA-\$3");
  entityName.replace("(.*?) \\($aka (.*?)\\), (.*)", "\$3 \$1 -AKA-\$2");
  entityName.replace("(.*?), (.*?) (.*?) \\(or (.*?)\\)", "\$2 \$3 \$1-AKA-\$2 \$4 \$1");
  entityName.replace("\\(aka (.*?)\\)", "-AKA-\$1");
  entityName.replace("\\(a\\.k\\.a\\. (.*?)\\)", "-AKA-\$1");
  entityName.replace("\\(aka (.*?)\$", "-AKA-\$1");
  entityName.replace("(?i)a\\.k\\.a(\\.|)", "-AKA-");
  entityName.replace("(?i) ALSO KNOWN AS ", "-AKA-");
  entityName.replace("(?i) DOING BUSINESS AS ", "-AKA-");
  // entityName.replace(" and ", "-AKA-");
  entityName.replace(" or ", "-AKA-");
  entityName.replace("(?i) o/a ", "-AKA-");
  entityName.replace("/", "-AKA-");
  entityName.replace("Chelmsford_SLASH_Dunnville", "Chelmsford/Dunnville")
  entityName.replace("_SAND_", "and");
  entityName.replace("Smith, James A., Dr.", "JAMES A. SMITH -AKA- DR. JAMES A. SMITH");
  entityName.replace("Berry, Dr. John", "JOHN BERRY -AKA- DR. JOHN BERRY");
  entityName.replace("Taylor, Jr., Lewis", "Lewis Taylor Jr.");
  entityName.replace("Taylor, Sr., Lewis", "Lewis Taylor Sr.");
  //context.info("Name after replace: " + entityName);

  // some additional cleanup
  entityName.replace("(.*?), (.*?) \\((.*?)\\)", "\$1 \$3, \$2"); // fixes this pattern... "Pinkett, Preston (II)"
  entityName.replace("\\(.*\\)", ""); // remove names withing parens
  entityName.replace("\".*\"", ""); // remove names within double quotes
  entityName.replace(",,", ","); // fixes double comma on list

  entityName.stripFormatting();
  def index = entityName.toString().indexOf("-AKA-");
  def aliases = null;
  if (index != -1) {
    aliases = entityName.toString().substring(index + 5).trim();
    entityName = entityName.toString().substring(0, index).trim();
  }
  if (hasComma && !(entityName.toString().trim() =~ /(?i) (Inc|Incorporated|L\.P|LP|LLLP|LLC|Ltd|Limited|Co|Corporation|Partnership|Corp|S\.A\. de C\.V|A\.V\.V\.|S\.A|S\.E\.C)(\.|)$/)) {
    type = 'P'
    entityName = context.formatName(entityName.toString().toUpperCase());
    if (entityName == 'DAVE') {
      entityName = 'CHANDRAMATTIE DAVE'
      aliases = 'RITA BAHADUR'
    }
    if (entityName == 'RAVINDRA DAVE') {
      aliases = 'DAVE RAVINDRA'
    }
  } else {
    type = 'O'
    if (entityName == 'RENE CO') {
      type = 'P'
    }
  }

  entity = context.findEntity([name: entityName]);
  if (!entity) {
    entity = context.getSession().newEntity();
    entity.setName(entityName.toString().toUpperCase().trim());
    entity.setType(type);
  }

  if (aliases) {
    aliases.split("-AKA-").each {alias ->
      alias = context.formatName(alias.toUpperCase());
      alias = alias.replaceAll("^[\\w]*\$", ""); // don't allow single name aliases
      if (StringUtils.isNotBlank(alias) && !entity.getAliases().contains(alias)) {
        entity.addAlias(alias);
      }
    }
  }

  ScrapeAddress address = new ScrapeAddress();
  address.setCountry("CANADA");
  if (!entity.getAddresses().contains(address)) {
    entity.addAddress(address);
  }

  for (int j = 0; j < sanctions.size(); j++) {
    def sanction = sanctions[j];
    def orderDate = orderDates[j];
    def bannedUntilDate = bannedUntilDates[j] != null ? bannedUntilDates[j] : "";
    def payment = payments[j];
    def violation = violations[j];
    def doc = docs[j];

    ScrapeEvent event = entity.newEvent();
    if (StringUtils.isNotBlank(orderDate.toString())) {
      event.setDate(context.parseDate(orderDate));

      testDate = sdf.parse(event.getDate());
      if (testDate.compareTo(today) > 0) {
        context.info("Rejecting date " + event.getDate() + " since in the future.")
        event.setDate(null);
      }
    }
    if (StringUtils.isNotBlank(bannedUntilDate.toString())) {
      event.setEndDate(context.parseDate(bannedUntilDate));
    }

    if (event.getDate() != null && event.getEndDate() != null) {
      try {
        sDate = sdf.parse(event.getDate());
        eDate = sdf.parse(event.getEndDate());
        if (sDate.compareTo(eDate) > 0) {
          context.info("Removing end date for " + entity.getName() + ": " + event.getEndDate() + " before " + event.getDate());
          event.setEndDate(null);
        }
      } catch (Exception e) {
        context.error("Date parse issue: ", e);
      }
    }

    def eventDescription = "";
    if (sanction!=null)
    sanction.replace("<[^>]*>", "; ");
    sanction.stripHtmlTags().stripFormatting().convertNbsp();
    if (StringUtils.isNotBlank(sanction.toString())) {
      sanction.replace("&lt;p&gt;", "");
      sanction.replace("&lt;/p&gt;", "; ");
      sanction.replace("&lt;br /&gt;", "; ");
      sanction.replace("; \$", "");
      sanction.replace("(?s)\\s+", " ");
      sanction.replace("(?i)&nbsp;", "")
      eventDescription = "Sanction: " + sanction.toString().trim();
      if (violation && violation.toString().trim() != '&nbsp;' && violation.toString().trim() != '') {
        violation.replace("<[^>]*>", " ");
        violation.replace("&lt;/p&gt;", "; ");
        violation.replace("&lt;br /&gt;", "; ");
        violation.replace("; \$", "");
        violation.replace("(?s)\\s+", " ");
        violation.replace("(?i)&nbsp;", "")
        eventDescription += "; Violation: " + violation.toString().trim();
      }
      if (payment && payment.toString().trim() != '&nbsp;' && payment.toString().trim() != '') {
        //println payment.length();
        payment.replace("&lt;p&gt;", "");
        payment.replace("&lt;/p&gt;", "; ");
        payment.replace("&lt;br /&gt;", "; ");
        payment.replace("; \$", "");
        payment.replace("\r\n", "");
        payment.replace("(?s)\\s+", " ");
        payment.replace("(?i)&nbsp;", "")
        payment.replace("(?i)<span[^>]+>[^<]+<\\/span>", "")
        eventDescription += "; Payment: " + payment.toString().trim();
      }
      eventDescription = eventDescription.replaceAll(/(;\s*)+/, '; ').trim();
      eventDescription = StringUtils.removeEnd(eventDescription, ":").trim()
      eventDescription = StringUtils.removeEnd(eventDescription, "\$").trim()
    } else {
      eventDescription = "Entity appears on the Canadian Securities Administrators Disciplined Persons site";
    }
    event.setDescription(eventDescription);

    if (doc) {
      def docMatch = doc =~ /<a href='(.*?)'[^.]*?>/
      while (docMatch.find()) {
        if (docMatch.group(1).startsWith("http:")) {
          def docUrl = docMatch.group(1).replaceAll("(?i)(?:&amp;amp;|&amp;)", "&")
          docUrl = StringUtils.removeEnd(docUrl, "\\").trim()
          entity.addUrl(docUrl);
        }
      }

    }
  }
  if (juris && juris.toString().trim() != '&nbsp;' && juris.toString().trim() != '') {
    entity.addRemark('Jurisdiction: ' + juris + "; Governing Body: " + jurisBody);
  }
  if (link) {
    link = link.toString().replaceAll(/(?i)(?:&amp;amp;|&amp;)/, "&");
    entity.addUrl(link);
  }
  //each item
}