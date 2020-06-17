/*
* cgeSaturationAdjust.cpp
*
*  Created on: 2013-12-26
*      Author: Wang Yang
*/

#include "cgeSaturationAdjust.h"

const static char* const s_fshSaturation = CGE_SHADER_STRING_PRECISION_M
(

const float GREEN_HUE = 0.26;
const float RED_HUE = 0.014;
const float OLIVE_HUE = 0.166;
const float BLUE_HUE = 0.66;
const float VIOLET_HUE = 0.83;
const float ORANGE_HUE = 0.036;

const float MAX_HUE_SHIFT = 0.04;
const float MAX_LUM_SHIFT = 0.09;
const float MAX_SATURATION_SHIFT = 0.25;

varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform vec4 green;
uniform vec4 red;
uniform vec4 all;

vec3 RGB2HSL(vec3 src)
{
	float maxc = max(max(src.r, src.g), src.b);
	float minc = min(min(src.r, src.g), src.b);
	float L = (maxc + minc) / 2.0;
	if(maxc == minc)
		return vec3(0.0, 0.0, L);
	float H, S;

	//////// Optimize ////////////////////////

// 	if(L < 0.5)
// 		S = (maxc - minc) / (maxc + minc);
// 	else
//		S = (maxc - minc) / (2.0 - maxc - minc);

	//注意， 某些低精度情况下 N - (A+B) != N - A - B
	float temp1 = maxc - minc;
	S = mix(temp1 / (2.0 - maxc - minc), temp1 / (maxc + minc), step(L, 0.5));

	//////// Optimize ////////////////////////

// 	if(maxc == src.r)
// 		H = (src.g - src.b) / (maxc - minc);
// 	else if(maxc == src.g)
// 		H = 2.0 + (src.b - src.r) / (maxc - minc);
// 	else
// 		H = 4.0 + (src.r - src.g) / (maxc - minc);

	vec3 comp;
	comp.xy = vec2(equal(src.xy, vec2(maxc)));
	float comp_neg = 1.0 - comp.x;
	comp.y *= comp_neg;
	comp.z = (1.0 - comp.y) * comp_neg;
	
	float dif = maxc - minc;
	vec3 result = comp * vec3((src.g - src.b) / dif,
		2.0 + (src.b - src.r) / dif,
		4.0 + (src.r - src.g) / dif);

	H = result.x + result.y + result.z;

	H *= 60.0;
	//if(H < 0.0) H += 360.0;
	H += step(H, 0.0) * 360.0;
	return vec3(H / 360.0, S, L); // H(0~1), S(0~1), L(0~1)
}

vec3 HSL2RGB(vec3 src) // H, S, L
{
// 	if(src.y <= 0.0)
// 		return vec3(src.z, src.z, src.z);
	float q = (src.z < 0.5) ? src.z * (1.0 + src.y) : (src.z + src.y - (src.y * src.z));
	float p = 2.0 * src.z - q;

	vec3 dst = vec3(src.x + 0.333, src.x, src.x - 0.333);

	//////// Optimize ////////////////////////

// 	if(dst.r < 0.0) dst.r += 1.0;
// 	else if(dst.r > 1.0) dst.r -= 1.0;
// 
// 	if(dst.g < 0.0) dst.g += 1.0;
// 	else if(dst.g > 1.0) dst.g -= 1.0;
// 
// 	if(dst.b < 0.0) dst.b += 1.0;
// 	else if(dst.b > 1.0) dst.b -= 1.0;

	//dst += step(dst, vec3(0.0));
	//dst -= step(vec3(1.0), dst);
	dst = fract(dst);

	//////// Optimize ////////////////////////

	//Plan A

// 	if(dst.r < 1.0 / 6.0)
// 		dst.r = p + (q - p) * 6.0 * dst.r;
// 	else if(dst.r < 0.5)
// 		dst.r = q;
// 	else if(dst.r < 2.0 / 3.0)
// 		dst.r = p + (q - p) * ((2.0 / 3.0) - dst.r) * 6.0;
// 	else dst.r = p;
// 
// 	if(dst.g < 1.0 / 6.0)
// 		dst.g = p + (q - p) * 6.0 * dst.g;
// 	else if(dst.g < 0.5)
// 		dst.g = q;
// 	else if(dst.g < 2.0 / 3.0)
// 		dst.g = p + (q - p) * ((2.0 / 3.0) - dst.g) * 6.0;
// 	else dst.g = p;
// 
// 	if(dst.b < 1.0 / 6.0)
// 		dst.b = p + (q - p) * 6.0 * dst.b;
// 	else if(dst.b < 0.5)
// 		dst.b = q;
// 	else if(dst.b < 2.0 / 3.0)
// 		dst.b = p + (q - p) * ((2.0 / 3.0) - dst.b) * 6.0;
// 	else dst.b = p;

	//Plan B

	vec3 weight = step(dst, vec3(1.0 / 6.0));
	vec3 weight_neg = 1.0 - weight;

	vec3 weight2 = weight_neg * step(dst, vec3(0.5));
	vec3 weight2_neg = weight_neg * (1.0 - weight2);

	vec3 weight3 = weight2_neg * step(dst, vec3(2.0 / 3.0));
	vec3 weight4 = (1.0 - weight3) * weight2_neg;

	float q_p = q - p;

	dst = mix(dst, p + q_p * 6.0 * dst, weight);
	dst = mix(dst, vec3(q), weight2);
	dst = mix(dst, p + q_p * ((2.0 / 3.0) - dst) * 6.0, weight3);
	dst = mix(dst, vec3(p), weight4);

	return dst;
}

vec3 adjustColor(vec3 src, vec4 all, vec4 green, vec4 red) //hue should be positive
{
	src = RGB2HSL(src);
	
	float hue = src.x;

	if(green.w != 0.0) {
		// check how far from skin hue
		float dist = hue - GREEN_HUE;
		if (dist > 0.5)
			dist -= 1.0;
		if (dist < -0.5)
			dist += 1.0;
		dist = abs(dist)/0.5;

		float weight = exp(-dist*dist*50.0);
		weight = clamp(weight, 0.0, 1.0);

		src.x += (green.x * weight * MAX_HUE_SHIFT);
		src.y += (green.y * weight * MAX_SATURATION_SHIFT);
		src.z += (green.z * weight * MAX_LUM_SHIFT);
	}
	if(red.w != 0.0) {
		// check how far from skin hue
		float dist = hue - RED_HUE;
		if (dist > 0.5)
			dist -= 1.0;
		if (dist < -0.5)
			dist += 1.0;
		dist = abs(dist)/0.5;

		float weight = exp(-dist*dist*50.0);
		weight = clamp(weight, 0.0, 1.0);

		src.x += (red.x * weight * MAX_HUE_SHIFT);
		src.y += (red.y * weight * MAX_SATURATION_SHIFT);
		src.z += (red.z * weight * MAX_LUM_SHIFT);
	}
	if(all.w != 0.0) {
		src.x += (all.x * MAX_HUE_SHIFT);
		src.y += (all.y * MAX_SATURATION_SHIFT);
		src.z += (all.z * MAX_LUM_SHIFT);
	}
	
	//src.x += green.x;
	//src.y += green.y;
	//src.z += green.z;
	return HSL2RGB(src);
}

void main()
{
	vec4 src = texture2D(inputImageTexture, textureCoordinate);
	src.rgb = adjustColor(src.rgb, all, green, red);
	gl_FragColor = src;
}
);

const static char* const s_fshSaturationFast = CGE_SHADER_STRING_PRECISION_M
(
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform float intensity;
void main()
{
	vec4 src = texture2D(inputImageTexture, textureCoordinate);
	float lum = (max(max(src.r, src.g),src.b) + min(min(src.r, src.g), src.b)) / 2.0;
	gl_FragColor = vec4(mix(vec3(lum), src.rgb, intensity), src.a);
}
);

const static char* const s_fshHSVAdjust = CGE_SHADER_STRING_PRECISION_M
(
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform vec3 vColor1;
uniform vec3 vColor2;

vec3 hsvAdjust(vec3 src, vec3 color1, vec3 color2) //color1:red green blue, color2: magenta yellow cyan
{
	float fmax = max(src.r,max(src.g,src.b));
	float fmin = min(src.r,min(src.g,src.b));
	float fdelta = fmax - fmin;
	float fs_off;
	vec3 hsv;
	hsv.z = fmax;
	if(0.0 == fdelta)
	{
		return src;
	}
	//hue calculate
	hsv.y = fdelta/fmax;
	if(fmax == src.r)
	{
		if(src.g >= src.b)
		{
			hsv.x = (src.g - src.b)/fdelta;
			fs_off = (color2.g - color1.r)*hsv.x + color1.r;
			//saturation adjust
			hsv.y = hsv.y*(1.0 + fs_off);
			clamp(hsv.y, 0.0, 1.0);
			//rgb2hsv end
			//hsv2rgb
			src.r = hsv.z;
			src.b = hsv.z*(1.0 - hsv.y);
			src.g = hsv.z*(1.0 - hsv.y + hsv.y*hsv.x);
		}
		else
		{
			hsv.x = (src.r - src.b)/fdelta;
			fs_off = (color1.r - color2.r)*hsv.x + color2.r;
			//saturation adjust
			hsv.y = hsv.y*(1.0 + fs_off);
			clamp(hsv.y, 0.0, 1.0);
			//rgb2hsv end
			//hsv2rgb
			src.r = hsv.z;
			src.g = hsv.z*(1.0 - hsv.y);
			src.b = hsv.z*(1.0 - hsv.y*hsv.x);
		}
	}
	else if(fmax == src.g)
	{

		if(src.r > src.b)
		{
			hsv.x = (src.g - src.r)/fdelta;
			fs_off = (color1.g - color2.g)*hsv.x + color2.g;
			//saturation adjust
			hsv.y = hsv.y*(1.0 + fs_off);
			clamp(hsv.y, 0.0, 1.0);
			//rgb2hsv end
			//hsv2rgb
			src.g = hsv.z;
			src.r = hsv.z*(1.0 - hsv.y*hsv.x);
			src.b = hsv.z*(1.0 - hsv.y);
		}
		else
		{
			//2
			hsv.x = (src.b - src.r)/fdelta;
			fs_off = (color2.b - color1.g)*hsv.x + color1.g;
			//saturation adjust
			hsv.y = hsv.y*(1.0 + fs_off);
			clamp(hsv.y, 0.0, 1.0);
			//rgb2hsv end
			//hsv2rgb
			src.g = hsv.z;
			src.r = hsv.z*(1.0 - hsv.y);
			src.b = hsv.z*(1.0 - hsv.y + hsv.y*hsv.x);
		}
	}
	else
	{
		if(src.g > src.r)
		{
			hsv.x = (src.b - src.g)/fdelta;
			fs_off = (color1.b - color2.b)*hsv.x + color2.b;
			//saturation adjust
			hsv.y = hsv.y*(1.0 + fs_off);
			clamp(hsv.y, 0.0, 1.0);
			//rgb2hsv end
			//hsv2rgb
			src.b = hsv.z;
			src.r = hsv.z*(1.0 - hsv.y);
			src.g = hsv.z*(1.0 - hsv.y*hsv.x);
		}
		else
		{
			//4
			hsv.x = (src.r - src.g)/fdelta;
			fs_off = (color2.r - color1.b)*hsv.x + color1.b;
			//saturation adjust
			hsv.y = hsv.y*(1.0 + fs_off);
			clamp(hsv.y, 0.0, 1.0);
			//rgb2hsv end
			//hsv2rgb
			src.b = hsv.z;
			src.r = hsv.z*(1.0 - hsv.y + hsv.y*hsv.x);
			src.g = hsv.z*(1.0 - hsv.y);
		}
	}
	return src;
}

void main()
{
	vec4 src = texture2D(inputImageTexture, textureCoordinate);
	src.rgb = hsvAdjust(src.rgb, vColor1, vColor2);
	gl_FragColor = src;
}
);

namespace CGE
{
	CGEConstString CGESaturationHSLFilter::paramSaturationName = "saturation";
	CGEConstString CGESaturationHSLFilter::paramHueName = "hue";
	CGEConstString CGESaturationHSLFilter::paramLuminanceName = "luminance";

	bool CGESaturationHSLFilter::init()
	{
		if(initShadersFromString(g_vshDefaultWithoutTexCoord, s_fshSaturation))
		{
			setGreen(0.0f, 0.0f, 0.0f, 0.0f);
			setRed(0.0f, 0.0f, 0.0f, 0.0f);
			setAll(0.0f, 0.0f, 0.0f, 0.0f);
			return true;
		}
		return false;
	}

    void CGESaturationHSLFilter::setAll(float isSet, float h, float s, float l)
    {
        m_program.bind();
        m_program.sendUniformf("all", h, s, l, isSet);
    }

	void CGESaturationHSLFilter::setGreen(float isSet, float h, float s, float l)
    {
        m_program.bind();
        m_program.sendUniformf("green", h, s, l, isSet);
    }

    void CGESaturationHSLFilter::setRed(float isSet, float h, float s, float l)
    {
        m_program.bind();
        m_program.sendUniformf("red", h, s, l, isSet);
    }
	
	void CGESaturationHSLFilter::setSaturation(float value)
	{
		m_program.bind();
		m_program.sendUniformf(paramSaturationName, value);
	}

	void CGESaturationHSLFilter::setHue(float value)
	{
		m_program.bind();
		m_program.sendUniformf(paramHueName, value);
	}

	void CGESaturationHSLFilter::setLum(float lum)
	{
		m_program.bind();
		m_program.sendUniformf(paramLuminanceName, lum);
	}

	///////// Saturation fast filter   ////////////////////////////////////

	CGEConstString CGESaturationFilter::paramIntensityName = "intensity";

	bool CGESaturationFilter::init()
	{
		if(initShadersFromString(g_vshDefaultWithoutTexCoord, s_fshSaturationFast))
		{
			setIntensity(1.0f);
			return true;
		}
		return false;
	}

	void CGESaturationFilter::setIntensity(float value)
	{
		m_program.bind();
		m_program.sendUniformf(paramIntensityName, value);
	}

	///////// Saturation Adjust for HSV  /////////////////////////////////////
	
	CGEConstString CGESaturationHSVFilter::paramColor1 = "vColor1";
	CGEConstString CGESaturationHSVFilter::paramColor2 = "vColor2";

	bool CGESaturationHSVFilter::init()
	{
		return initShadersFromString(g_vshDefaultWithoutTexCoord, s_fshHSVAdjust);
	}

	void CGESaturationHSVFilter::setAdjustColors(float red, float green, float blue, float magenta, float yellow, float cyan)
	{
		m_program.bind();
		m_program.sendUniformf(paramColor1, red, green, blue);
		m_program.sendUniformf(paramColor2, magenta, yellow, cyan);
	}

}