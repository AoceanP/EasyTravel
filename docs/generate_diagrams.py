"""Generates EasyTravel's architecture and saga-sequence diagrams as draw.io files."""
import sys
from xml.sax.saxutils import escape

OUT = sys.argv[1]

BLUE = "fillColor=#dae8fc;strokeColor=#6c8ebf;"
GREEN = "fillColor=#d5e8d4;strokeColor=#82b366;"
ORANGE = "fillColor=#ffe6cc;strokeColor=#d79b00;"
PURPLE = "fillColor=#e1d5e7;strokeColor=#9673a6;"
YELLOW = "fillColor=#fff2cc;strokeColor=#d6b656;"
RED = "fillColor=#f8cecc;strokeColor=#b85450;"
GREY = "fillColor=#f5f5f5;strokeColor=#666666;"


class Diagram:
    def __init__(self, name, w, h):
        self.name, self.w, self.h, self.cells, self.n = name, w, h, [], 0

    def _id(self):
        self.n += 1
        return f"c{self.n}"

    def box(self, x, y, w, h, label, style):
        i = self._id()
        self.cells.append(
            f'<mxCell id="{i}" value="{escape(label, {chr(34): "&quot;"})}" style="whiteSpace=wrap;html=1;{style}" '
            f'vertex="1" parent="1"><mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')
        return i

    def edge(self, src, dst, label="", style="", exit=None, entry=None):
        i = self._id()
        pts = ""
        if exit:
            pts += f"exitX={exit[0]};exitY={exit[1]};exitDx=0;exitDy=0;"
        if entry:
            pts += f"entryX={entry[0]};entryY={entry[1]};entryDx=0;entryDy=0;"
        self.cells.append(
            f'<mxCell id="{i}" value="{escape(label, {chr(34): "&quot;"})}" style="endArrow=block;html=1;rounded=0;'
            f'labelBackgroundColor=#ffffff;fontSize=11;{pts}{style}" edge="1" parent="1" source="{src}" target="{dst}">'
            f'<mxGeometry relative="1" as="geometry"/></mxCell>')

    def line(self, x1, y1, x2, y2, label="", style=""):
        i = self._id()
        self.cells.append(
            f'<mxCell id="{i}" value="{escape(label, {chr(34): "&quot;"})}" style="html=1;rounded=0;labelBackgroundColor=#ffffff;'
            f'fontSize=11;{style}" edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
            f'<mxPoint x="{x1}" y="{y1}" as="sourcePoint"/><mxPoint x="{x2}" y="{y2}" as="targetPoint"/>'
            f'</mxGeometry></mxCell>')

    def save(self, path):
        body = "".join(self.cells)
        xml = (f'<mxfile host="app.diagrams.net"><diagram id="{self.name}" name="{self.name}">'
               f'<mxGraphModel dx="{self.w}" dy="{self.h}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" '
               f'arrows="1" fold="1" page="1" pageScale="1" pageWidth="{self.w}" pageHeight="{self.h}" math="0" shadow="0">'
               f'<root><mxCell id="0"/><mxCell id="1" parent="0"/>{body}</root></mxGraphModel></diagram></mxfile>')
        with open(path, "w", encoding="utf-8") as f:
            f.write(xml)


# ---------------------------------------------------------------- architecture
a = Diagram("architecture", 1240, 880)
a.box(20, 20, 1200, 30, "<b>EasyTravel</b>: five Spring Boot services, one message broker (all demo data is fake)",
      "text;align=left;fontSize=15;strokeColor=none;fillColor=none;")
client = a.box(560, 70, 200, 60, "<b>Client</b><br>curl / browser", "ellipse;" + PURPLE)
gw = a.box(530, 170, 260, 70, "<b>API Gateway</b> :8080<br><i>one front door, routes /api/**</i>", BLUE)
eureka = a.box(40, 310, 200, 100, "<b>Discovery Server</b> :8761<br>Eureka<br><i>who is running where</i>", GREY)
inv = a.box(290, 310, 220, 100, "<b>Inventory Service</b> :8081<br>search, rank, quote<br><i>5 mock flights</i>", GREEN)
book = a.box(640, 310, 240, 100, "<b>Booking Service</b> :8082<br>prices bookings server-side<br><i>own H2 database</i>", GREEN)
pay = a.box(940, 310, 240, 100, "<b>Payment Service</b> :8083<br>mock card, declines over 2000.00<br><i>own H2 database</i>", GREEN)

a.edge(client, gw, "HTTP JSON")
a.edge(gw, inv, "/api/flights/**", exit=(0.1, 1), entry=(0.5, 0))
a.edge(gw, book, "/api/bookings/**", exit=(0.6, 1), entry=(0.5, 0))
a.edge(gw, pay, "/api/payments/**", exit=(1, 0.5), entry=(0.5, 0), style="edgeStyle=orthogonalEdgeStyle;")
a.edge(book, inv, "price quote,<br>by service name", exit=(0, 0.5), entry=(1, 0.5))
a.edge(gw, eureka, "every service registers here,<br>callers look names up", exit=(0, 0.5), entry=(0.5, 0),
       style="dashed=1;edgeStyle=orthogonalEdgeStyle;strokeColor=#666666;")

OR = "strokeColor=#d79b00;"
a.box(620, 470, 580, 390, "<b>RabbitMQ</b> :5672 (UI :15672)",
      "rounded=1;dashed=1;fillColor=none;strokeColor=#d79b00;verticalAlign=top;align=right;spacingRight=14;spacingTop=4;")
bx = a.box(700, 560, 160, 56, "booking.exchange<br><i>booking.created</i>", ORANGE)
bq = a.box(940, 560, 140, 56, "payment.booking-<br>created.queue", ORANGE)
pq = a.box(640, 680, 220, 56, "booking.payment-<br>completed.queue", ORANGE)
px = a.box(940, 680, 240, 56, "payment.exchange<br><i>payment.completed</i>", ORANGE)
a.box(640, 780, 540, 56, "easytravel.dlx → *.dlq<br><i>a message that still fails after 3 tries is parked here, not retried forever</i>", RED)
a.edge(book, bx, "1", exit=(0.6, 1), entry=(0.525, 0), style=OR)
a.edge(bx, bq, "", style=OR)
a.edge(bq, pay, "2", exit=(0.5, 0), entry=(0.2917, 1), style=OR)
a.edge(pay, px, "3", exit=(0.9, 1), entry=(0.9, 0), style=OR)
a.edge(px, pq, "", style=OR)
a.edge(pq, book, "4", exit=(0.0909, 0), entry=(0.0833, 1), style=OR)

a.box(40, 470, 540, 390,
      "<b>The booking saga</b><br><br>"
      "<b>1.</b> Booking saves the booking as PENDING, answers the client 201 at once, "
      "then publishes <i>BookingCreatedEvent</i> (only after the database commit).<br><br>"
      "<b>2.</b> Payment consumes it, skips it if this booking was already charged, "
      "otherwise charges the mock card.<br><br>"
      "<b>3.</b> Payment publishes <i>PaymentEvent</i>: success, or failure with a reason.<br><br>"
      "<b>4.</b> Booking consumes it: PENDING becomes CONFIRMED or PAYMENT_FAILED. "
      "A duplicate or late event never changes a decided booking.<br><br>"
      "<b>Why a broker?</b> If payment-service is down, events wait in the durable "
      "queue and are processed when it comes back.",
      "text;align=left;verticalAlign=top;fontSize=12;spacing=10;" + GREY)
a.save(f"{OUT}/easytravel-architecture.drawio")

# ---------------------------------------------------------------- saga sequence
s = Diagram("saga", 1300, 1080)
s.box(20, 10, 1260, 30, "<b>Booking saga</b>: what happens after <i>POST /api/bookings</i>",
      "text;align=left;fontSize=15;strokeColor=none;fillColor=none;")
names = [("Client", PURPLE), ("API Gateway", BLUE), ("Booking Service", GREEN),
         ("Inventory Service", GREEN), ("RabbitMQ", ORANGE), ("Payment Service", GREEN)]
X = {}
for k, (n, c) in enumerate(names):
    x = 40 + k * 210
    s.box(x, 50, 180, 44, f"<b>{n}</b>", c)
    X[n] = x + 90
    s.line(x + 90, 94, x + 90, 1060, "", "endArrow=none;dashed=1;strokeColor=#999999;")

y = [130]


def msg(a_, b_, text, reply=False, gap=58, color=""):
    yy = y[0]
    style = ("dashed=1;endArrow=open;" if reply else "endArrow=block;") + color
    s.line(X[a_], yy, X[b_], yy, text, style)
    y[0] += gap


def note(who, text, style=YELLOW, h=46, gap=66, w=240):
    s.box(X[who] - w // 2, y[0] - 18, w, h, text, style + "fontSize=11;")
    y[0] += gap


msg("Client", "API Gateway", "POST /api/bookings {flight, cabin}<br><i>no price: the server sets it</i>")
msg("API Gateway", "Booking Service", "routed via Eureka", gap=50)
msg("Booking Service", "Inventory Service", "GET /api/flights/quote")
msg("Inventory Service", "Booking Service", "base fare 210.00", reply=True)
note("Booking Service", "price = fare × cabin (ECONOMY 1, BUSINESS 2.5, FIRST 5)<br>save booking as <b>PENDING</b>", w=300, h=50)
msg("Booking Service", "Client", "201 Created, Location: /api/bookings/{uuid}, status PENDING", reply=True, gap=62)
note("Booking Service", "after the DB commit only:<br>no event for a booking that was rolled back", style=GREY, w=280)
msg("Booking Service", "RabbitMQ", "BookingCreatedEvent {id, user, amount}", color="strokeColor=#d79b00;")
msg("RabbitMQ", "Payment Service", "deliver (at least once)", color="strokeColor=#d79b00;")
note("Payment Service", "already paid for this booking? skip.<br>else charge the mock card, save,<br>report success or failure + reason", w=250, h=60, gap=76)
msg("Payment Service", "RabbitMQ", "PaymentEvent", color="strokeColor=#d79b00;")
msg("RabbitMQ", "Booking Service", "deliver", color="strokeColor=#d79b00;")
note("Booking Service", "still PENDING? → <b>CONFIRMED</b> or <b>PAYMENT_FAILED</b><br>already decided? ignore the duplicate",
     w=300, h=50)
msg("Client", "API Gateway", "GET /api/bookings/{uuid} (poll)", gap=50)
msg("API Gateway", "Booking Service", "", gap=40)
msg("Booking Service", "Client", "200 status CONFIRMED (or PAYMENT_FAILED with reason)", reply=True, gap=40)
s.save(f"{OUT}/easytravel-saga.drawio")
print("written", y[0])
