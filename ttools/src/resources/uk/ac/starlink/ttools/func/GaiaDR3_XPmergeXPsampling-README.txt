Version: 1.0
Date: 2022-05-25

The file XpSampling_v375wiv142r.csv contains the evaluation of the inverted bases on the default wavelength grid. The file 
is formatted as follows (the line number within each file where each quantity is given are reported in the square parenthesis 
at the start of the item description):
	[L0] The header reports the following parameters, separated by commas:
            the solution identifier 4545469030156206114 which links this configuration to the Gaia release (see 
	    https://gea.esac.esa.int/archive/documentation/GDR3/Gaia_archive/chap_datamodel/sec_dm_main_tables/ssec_dm_gaia_source.html#gaia_source-solution_id);
            the number of wavelength grid points in the default sampling (indicated by N in the following);
            the number of inverted bases for BP (this is equal to 55 for Gaia DR3);
            the number of inverted bases for RP (this is equal to 55 for Gaia DR3).
        [L1:N] The matrix providing the inverted bases for BP sampled onto the default grid: column k provides the k-th 
	inverted basis sampled at the N sampling grid points. Different matrix rows appear in different rows in the file. 
	The 55 elements in each row are separated by commas.
        [L(N+1):2N] The matrix providing the inverted bases for RP sampled onto the default grid: column k provides the 
	k-th inverted basis sampled at the N sampling grid points. Different matrix rows appear in different rows in the 
	file. The elements in each row are separated by commas.

The file XpMerge_v375wiv142r.csv contains the BP/RP merge instructions, that allow combining the BP and RP spectra into a 
unique absolute spectrum. The file is formatted as follows:

        [L0] The header reports the following parameters, separated by commas:
            the solution identifier 4545469030156206114 which links this configuration to the Gaia release;
            the number of wavelength grid points in the default sampling (indicated by N in the following).
        [L1] The default wavelength sampling grid as an array of N elements separated by commas.
        [L2] The array of weights to be applied to the BP flux at each grid point when combining the BP and RP 
	contributions. This will have value equal to 1 in the wavelength range that is only covered by BP and 0 in the 
	range only covered by RP, while it will assume values in the range ]0,1[ in the wavelength range where BP and 
	RP overlap.
        [L3] The array of weights to be applied to the RP flux at each grid point when combining the BP and RP 
	contributions. This will have value equal to 0 in the wavelength range that is only covered by BP and 1 in the 
	range only covered by RP, while it will assume values in the range ]0,1[ in the wavelength range where BP and 
	RP overlap.

When opening the file XpMerge_v375wiv142r.csv with Excel the first line can provide the wrong data. This is caused by a known bug in Excel. To avoid mistakes, we copy here the first line: 4545469030156206114,343 of the file XpMerge_v375wiv142r.csv.


These files have been prepared by Coordination Unit 5 of the Gaia Data Processing and Analysis Consortium. If you use 
this data please cite the paper "Gaia Data Release 3: External calibration of BP/RP low-resolution spectroscopic data" by
Montegriffo et al. 2022.
Credits: ESA/Gaia/DPAC and the CU5/DPCI/PhotPipe team.
