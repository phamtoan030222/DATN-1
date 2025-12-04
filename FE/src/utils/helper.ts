import dayjs from "dayjs"

export const convertMsToYear = (ms: number) => {
    const data = dayjs(ms);

    return data.year();
}

export const convertYearToMs = (year: number) => {
    const time = dayjs(`${year}-01-01`);
    return time.valueOf();
}

export const toBlob = (data: any, options: BlobPropertyBag = {type: 'application/json'}): Blob => {
    return new Blob([JSON.stringify(data)], options)
}