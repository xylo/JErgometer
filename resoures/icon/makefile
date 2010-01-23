
sizes := 16 24 32 64

default: pngs

pngs: icon.svg
	for i in $(sizes); do convert icon.svg -trim +repage -resize $$ix$$i icon_$$ix$$i.png; done

clean:
	@rm *.png >/dev/null 2>&1 || true
