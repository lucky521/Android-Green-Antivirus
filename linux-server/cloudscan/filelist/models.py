from django.db import models


# Create your models here.
from django.contrib import admin

_list_per_page = 50

class white_list(models.Model):
	fileID = models.CharField(max_length=33)
	status = models.CharField(max_length=1)
	



