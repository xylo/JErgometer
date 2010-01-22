
texs := $(wildcard record*.tex stop*.tex)
dvis := $(texs:%.tex=%.dvi)
pss  := $(texs:%.tex=%.ps)
pdfs := $(texs:%.tex=%.pdf)
pngs := $(texs:%.tex=%.png)

size := 20x20

default: pngs clean

pdfs: $(pdfs)

pngs: $(pngs)

%.dvi: %.tex
	latex $*.tex

%.ps: %.dvi
	dvips $*

%.pdf: %.tex
	pdflatex $*.tex
	@rm $*.aux $*.log  >/dev/null 2>&1 || true

# %.png: %.pdf
# 	convert $*.pdf -trim -resize $(size) $*.png

%.png: %.ps
	gs -q -dNOPAUSE -dBATCH -sDEVICE=pngalpha -r160 -dEPSCrop -sOutputFile=$*-tmp1.png $*.ps
	convert $*-tmp1.png -trim +repage -resize $(size) $*.png
	rm $*-tmp1.png

clean:
	@rm *.aux *.bbl *.blg *.log *.dvi >/dev/null 2>&1 || true

clean-all: clean
	@rm $(pdfs) $(pss) $(pngs) >/dev/null 2>&1 || true

# for i in `ls *.tex`; do
#   file=`basename $i .tex`;
#   echo $file;
#   pdflatex $file.tex
#   convert $file.pdf -trim -resize 20x20 $file.png
# done
