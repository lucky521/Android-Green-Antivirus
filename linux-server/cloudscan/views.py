from django.http import HttpResponse
from filelist.models import white_list
from django.template import Template, Context
from django import forms


last_md5 = '' #remember the md5str that uploaded just now

class UploadFileForm(forms.Form):
	title = forms.CharField(max_length=50)
	file  = forms.FileField()

def handle_uploaded_md5(f):
	global last_md5
	filename = str(f)
	md5str = ''
	destination = open("uploadfile/" + filename, 'w')
	for chunk in f.chunks():
		destination.write(chunk)
		md5str += chunk
	destination.close()
	
	try:
		#check this md5str in Database
		findit = white_list.objects.filter(fileID = md5str)
		if len(findit) == 0: #if not exit
			last_md5 = md5str	# client will upload the file soon
			return "new_file"
		else: #if exit
			#check status of the file
			for i in findit.values('status'):	#valuesqueryset!!!
				this_status = i['status']
			if this_status == 'y':
				return "healthy file"
			else:
				return "virus"
	except Exception as e:
		print "[*]error!"
		print e	


def handle_uploaded_file(f):
	global last_md5
	filename = str(f)
	md5str = ''
	destination = open("uploadfile/" + filename, 'w')
	for chunk in f.chunks():
		destination.write(chunk)
	destination.close()

	#scan this file
	import pyclamav
	print 'scanning...'
	scanret = pyclamav.scanfile("uploadfile/" + filename)

	if scanret[0]: #if 1, virus
		scanresult = 'n'
	else:
		scanresult = 'y'
	#if or not virus
	try:	
		#save to database
		p = white_list(fileID = last_md5, status = scanresult)
		p.save()
	except Exception as e:
		print "[*]error!"
		print e

	print scanret
	if scanresult == 'y':
		return "healthy file"
	else:
		return "virus"


####################################################################
def md5handle(request):
	try:
		if request.method == 'POST':
			print
			print '### Receive md5 ########################################'
			print request.FILES['file']
			result = handle_uploaded_md5(request.FILES['file'])
			
	except Exception as e:
		print "[*]error!"
		print e
	return HttpResponse(result)

def filehandle(request):
	try:
		if request.method == 'POST':
			print
			print '### Receive File ########################################'
			print request.FILES['file']
			result = handle_uploaded_file(request.FILES['file'])
			
	except Exception as e:
		print "[*]error!"
		print e
	return HttpResponse(result)

