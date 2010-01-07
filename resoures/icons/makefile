
texs := $(wildcard record*.tex stop*.tex)
pdfs := $(texs:%.tex=%.pdf)
pngs := $(texs:%.tex=%.png)

default: pngs

pdfs: $(pdfs)

pngs: $(pngs)

%.pdf: %.tex
	pdflatex $*.tex
	@rm $*.aux $*.log  >/dev/null 2>&1 || true

%.png: %.pdf
	convert $*.pdf -trim -resize 20x20 $*.png

clean:
	@rm *.aux *.bbl *.blg *.log >/dev/null 2>&1 || true

clean-all:
	@rm $(pdfs) $(pngs) >/dev/null 2>&1 || true

# for i in `ls *.tex`; do
#   file=`basename $i .tex`;
#   echo $file;
#   pdflatex $file.tex
#   convert $file.pdf -trim -resize 20x20 $file.png
# done
